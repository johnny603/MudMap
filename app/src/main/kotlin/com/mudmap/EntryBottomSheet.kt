package com.mudmap

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import com.mudmap.storage.TrailEntry

internal fun Activity.showEntrySheet(entry: TrailEntry) {
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
