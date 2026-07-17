package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // لو المستخدم مسجل دخول أصلاً، ما نعرض شاشة تسجيل الدخول، نوديه مباشرة للرئيسية
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val languageButton = findViewById<Button>(R.id.languageButton)

        updateLanguageButtonText(languageButton)
        languageButton.setOnClickListener {
            toggleAppLanguage()
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, getString(R.string.error_generic, task.exception?.message), Toast.LENGTH_LONG).show()
                    }
                }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun updateLanguageButtonText(button: Button) {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val isEnglish = !currentLocales.isEmpty && currentLocales[0]?.language == "en"
        button.text = if (isEnglish) "العربية" else "English"
    }

    private fun toggleAppLanguage() {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val isEnglish = !currentLocales.isEmpty && currentLocales[0]?.language == "en"
        val newLocale = if (isEnglish) {
            LocaleListCompat.forLanguageTags("ar")
        } else {
            LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(newLocale)
    }
}
