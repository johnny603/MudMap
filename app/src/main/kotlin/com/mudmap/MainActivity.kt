package com.mudmap

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.room.Room
import com.mudmap.adapters.AudioDecoder
import com.mudmap.adapters.WhisperCppAdapter
import com.mudmap.extractors.RuleBasedExtractor
import com.mudmap.location.LocationProvider
import com.mudmap.service.LogEntryUseCase
import com.mudmap.storage.MudMapDatabase
import com.mudmap.storage.TrailEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var database: MudMapDatabase
    private lateinit var recorder: RecorderController
    private lateinit var location: LocationProvider
    private lateinit var logger: LogEntryUseCase
    private lateinit var mapView: MapView
    private lateinit var map: MapViewController
    private lateinit var button: Button
    private lateinit var status: TextView
    private var recording = false
    private var askedPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        database = Room.databaseBuilder(this, MudMapDatabase::class.java, "mudmap.db").build()
        recorder = RecorderController(this)
        location = LocationProvider(this, scope)
        logger = LogEntryUseCase(
            WhisperCppAdapter(this, AudioDecoder()),
            RuleBasedExtractor(this), database.entryDao()
        )
        mapView = MapView(this)
        mapView.onCreate(savedInstanceState)
        button = Button(this).apply { text = "Record" }
        status = TextView(this).apply {
            text = "Ready"
            setPadding(24, 12, 24, 12)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(mapView, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(status, LinearLayout.LayoutParams(-1, -2))
            addView(button, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(root)
        map = MapViewController(mapView, ::showEntry)
        button.setOnClickListener { if (recording) stopRecording() else startRecording() }
        scope.launch {
            try { map.reload(database.entryDao().getAll()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { status.text = "Load failed: ${error.message}" }
        }
    }

    private fun startRecording() {
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val gps = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!mic || (!gps && !askedPermissions)) {
            askedPermissions = true
            requestPermissions(arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 1)
            status.text = "Choose permissions, then press Record again."
            return
        }
        try {
            recorder.start()
            location.requestOnce()
            recording = true
            button.text = "Stop"
            status.text = "Recording"
        } catch (error: Exception) {
            status.text = "Record failed: ${error.message}"
        }
    }

    private fun stopRecording() {
        recording = false
        button.text = "Record"
        val path = try { recorder.stop() } catch (error: Exception) {
            location.last()
            status.text = "Stop failed: ${error.message}"
            return
        }
        val point = location.last()
        button.isEnabled = false
        status.text = "Transcribing and saving"
        scope.launch {
            try {
                val entry = logger.log(path, point)
                status.text = if (entry.geoPoint == null) "Saved without GPS" else "Saved"
                map.reload(database.entryDao().getAll())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                status.text = "Processing failed; audio retained: ${error.message}"
            } finally { button.isEnabled = true }
        }
    }

    private fun showEntry(entry: TrailEntry) {
        val content = TextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 18f
            setPadding(32, 32, 32, 32)
            text = "${entry.transcript}\n\nSpecies: " +
                entry.species.joinToString().ifEmpty { "None" } +
                "\nConditions: " + entry.conditions.joinToString().ifEmpty { "None" }
        }
        val sheet = Dialog(this)
        sheet.setContentView(ScrollView(this).apply { addView(content) })
        sheet.show()
        sheet.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        scope.cancel(); recorder.release(); mapView.onDestroy()
        database.close(); super.onDestroy()
    }
}
