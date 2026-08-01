package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.BounceInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val verseText = findViewById<TextView>(R.id.verseText)
        val dropText = findViewById<TextView>(R.id.splashDropText)
        val appNameText = findViewById<TextView>(R.id.splashAppNameText)

        val handler = Handler(Looper.getMainLooper())

        // المرحلة 1: ظهور الآية تدريجيًا
        verseText.animate()
            .alpha(1f)
            .setDuration(1200)
            .start()

        // المرحلة 2: بعد ظهورها، تبقى ثابتة، ثم تختفي تدريجيًا
        handler.postDelayed({
            verseText.animate()
                .alpha(0f)
                .setDuration(900)
                .withEndAction {
                    // المرحلة 3: نزول قطرة الدم بحركة نطّة
                    dropText.translationY = -300f
                    dropText.alpha = 1f
                    dropText.animate()
                        .translationY(0f)
                        .setDuration(900)
                        .setInterpolator(BounceInterpolator())
                        .start()

                    // المرحلة 4: ظهور اسم التطبيق بعد استقرار القطرة
                    handler.postDelayed({
                        appNameText.animate()
                            .alpha(1f)
                            .setDuration(700)
                            .start()
                    }, 900)

                    // المرحلة 5: الانتقال التلقائي بعد اكتمال العرض
                    handler.postDelayed({
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }, 2400)
                }
                .start()
        }, 3200)
    }
}
