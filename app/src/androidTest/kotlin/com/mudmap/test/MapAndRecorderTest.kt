package com.mudmap.test

import android.media.MediaExtractor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mudmap.MainActivity
import com.mudmap.RecorderController
import com.mudmap.adapters.AudioDecoder
import com.mudmap.storage.GeoPoint
import com.mudmap.storage.MudMapDatabase
import com.mudmap.storage.TrailEntry
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@RunWith(AndroidJUnit4::class)
class MapAndRecorderTest {
    @Test
    fun loadsLocalTilesAndOpensPinBottomSheet() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val database = Room.databaseBuilder(context, MudMapDatabase::class.java, "mudmap.db").build()
        try {
            database.entryDao().insert(TrailEntry(
                recordedAt = System.currentTimeMillis(), geoPoint = GeoPoint(37.77, -122.44),
                transcript = "saw a heron by the creek", species = listOf("heron"),
                conditions = listOf("creek")
            ))
        } finally { database.close() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val ready = AtomicBoolean(false)
            var x = 0f
            var y = 0f
            for (attempt in 0 until 100) {
                scenario.onActivity { activity ->
                    val root = activity.findViewById<ViewGroup>(android.R.id.content)
                    val mapView = (root.getChildAt(0) as LinearLayout).getChildAt(0) as MapView
                    mapView.getMapAsync { map ->
                        if (map.style?.isFullyLoaded == true && map.markers.isNotEmpty()) {
                            assertTrue(map.style?.getSource("region") != null)
                            val point = map.projection.toScreenLocation(LatLng(37.77, -122.44))
                            val offset = IntArray(2)
                            mapView.getLocationOnScreen(offset)
                            x = point.x + offset[0]
                            y = point.y + offset[1] - 10f
                            ready.set(true)
                        }
                    }
                }
                if (ready.get()) break
                Thread.sleep(300)
            }
            assertTrue("Offline style and saved marker did not load.", ready.get())
            val now = SystemClock.uptimeMillis()
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                val event = MotionEvent.obtain(now, now + action * 30L, action, x, y, 0)
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                instrumentation.sendPointerSync(event)
                event.recycle()
            }
            Thread.sleep(500)
            assertTrue(instrumentation.uiAutomation.rootInActiveWindow
                .findAccessibilityNodeInfosByText("Species: heron").isNotEmpty())
        }
    }

    @Test
    fun recordsPrivateM4aAndDecodesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch(MainActivity::class.java).use {
            val recorder = RecorderController(context)
            val path: String
            try {
                recorder.start()
                Thread.sleep(1800)
                path = recorder.stop()
            } finally { recorder.release() }
            assertTrue(path.startsWith(context.filesDir.absolutePath))
            assertTrue(path.endsWith(".m4a") && File(path).length() > 0)
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                assertTrue(extractor.trackCount > 0)
            } finally { extractor.release() }
            assertTrue(AudioDecoder().decode(path).isNotEmpty())
        }
    }
}
