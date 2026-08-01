package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var emailLoginLayout: LinearLayout
    private lateinit var phoneLoginLayout: LinearLayout
    private lateinit var emailTabButton: Button
    private lateinit var phoneTabButton: Button

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    private lateinit var phoneNumberInput: EditText
    private lateinit var sendOtpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        auth = FirebaseAuth.getInstance()

        // لو المستخدم مسجل دخول أصلاً، ما نعرض شاشة تسجيل الدخول، نوديه مباشرة للرئيسية
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        emailLoginLayout = findViewById(R.id.emailLoginLayout)
        phoneLoginLayout = findViewById(R.id.phoneLoginLayout)
        emailTabButton = findViewById(R.id.emailTabButton)
        phoneTabButton = findViewById(R.id.phoneTabButton)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)

        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        sendOtpButton = findViewById(R.id.sendOtpButton)

        val registerButton = findViewById<Button>(R.id.registerButton)
        val languageButton = findViewById<Button>(R.id.languageButton)

        updateLanguageButtonText(languageButton)
        languageButton.setOnClickListener {
            toggleAppLanguage()
        }

        showEmailTab()

        emailTabButton.setOnClickListener { showEmailTab() }
        phoneTabButton.setOnClickListener { showPhoneTab() }

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

        sendOtpButton.setOnClickListener {
            val rawNumber = phoneNumberInput.text.toString().trim()

            if (rawNumber.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullNumber = formatSaudiPhoneNumber(rawNumber)

            if (fullNumber == null) {
                Toast.makeText(this, getString(R.string.invalid_phone_number), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendOtpButton.isEnabled = false
            startPhoneVerification(fullNumber)
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showEmailTab() {
        emailLoginLayout.visibility = View.VISIBLE
        phoneLoginLayout.visibility = View.GONE
        emailTabButton.isEnabled = false
        phoneTabButton.isEnabled = true
    }

    private fun showPhoneTab() {
        emailLoginLayout.visibility = View.GONE
        phoneLoginLayout.visibility = View.VISIBLE
        emailTabButton.isEnabled = true
        phoneTabButton.isEnabled = false
    }

    // يحول أي صيغة إدخال (05xxxxxxxx أو 5xxxxxxxx أو +9665xxxxxxxx) لصيغة دولية سعودية موحدة
    private fun formatSaudiPhoneNumber(input: String): String? {
        var digits = input.replace(" ", "").replace("-", "")

        if (digits.startsWith("+966")) {
            digits = digits.removePrefix("+966")
        } else if (digits.startsWith("966")) {
            digits = digits.removePrefix("966")
        } else if (digits.startsWith("0")) {
            digits = digits.removePrefix("0")
        }

        if (!digits.startsWith("5") || digits.length != 9) {
            return null
        }

        return "+966$digits"
    }

    private fun startPhoneVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    sendOtpButton.isEnabled = true
                    Toast.makeText(this@LoginActivity, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    sendOtpButton.isEnabled = true
                    PhoneAuthHolder.resendToken = token
                    PhoneAuthHolder.phoneNumber = phoneNumber

                    val intent = Intent(this@LoginActivity, OtpVerifyActivity::class.java)
                    intent.putExtra("verificationId", verificationId)
                    intent.putExtra("phoneNumber", phoneNumber)
                    startActivity(intent)
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
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
