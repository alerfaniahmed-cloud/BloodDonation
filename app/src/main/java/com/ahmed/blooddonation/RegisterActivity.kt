package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val accountTypeGroup = findViewById<RadioGroup>(R.id.accountTypeGroup)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val languageButton = findViewById<Button>(R.id.languageButton)

        updateLanguageButtonText(languageButton)
        languageButton.setOnClickListener {
            toggleAppLanguage()
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val accountType = if (accountTypeGroup.checkedRadioButtonId == R.id.radioHospital) {
                "hospital"
            } else {
                "individual"
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userData = hashMapOf(
                                "accountType" to accountType,
                                "email" to email
                            )
                            firestore.collection("users").document(userId)
                                .set(userData)
                                .addOnCompleteListener {
                                    Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, ProfileActivity::class.java))
                                    finish()
                                }
                        } else {
                            Toast.makeText(this, getString(R.string.account_created), Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ProfileActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.error_generic, task.exception?.message), Toast.LENGTH_LONG).show()
                    }
                }
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
