package com.ahmed.blooddonation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val textView = TextView(this)
        textView.text = "مرحباً بك في تطبيق دم 🌹"
        textView.textSize = 22f
        textView.gravity = Gravity.CENTER

        layout.addView(textView)
        setContentView(layout)
    }
}
