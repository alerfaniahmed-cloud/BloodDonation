package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpVerifyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String = ""
    private var phoneNumber: String = ""

    private lateinit var otpMessageText: TextView
    private lateinit var otpCodeInput: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendButton: Button

    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verify)

        auth = FirebaseAuth.getInstance()

        verificationId = intent.getStringExtra("verificationId") ?: ""
        phoneNumber = intent.getStringExtra("phoneNumber") ?: (PhoneAuthHolder.phoneNumber ?: "")

        otpMessageText = findViewById(R.id.otpMessageText)
        otpCodeInput = findViewById(R.id.otpCodeInput)
        verifyButton = findViewById(R.id.verifyButton)
        resendButton = findViewById(R.id.resendButton)

        otpMessageText.text = getString(R.string.otp_sent_message, phoneNumber)

        verifyButton.setOnClickListener {
            val code = otpCodeInput.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (verificationId.isEmpty()) {
                Toast.makeText(this, getString(R.string.otp_verification_failed), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyButton.isEnabled = false
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithCredential(credential)
        }

        resendButton.setOnClickListener {
            resendOtpCode()
        }

        startResendTimer()
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                verifyButton.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.invalid_otp_code), Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun resendOtpCode() {
        val token = PhoneAuthHolder.resendToken

        if (phoneNumber.isEmpty() || token == null) {
            Toast.makeText(this, getString(R.string.otp_verification_failed), Toast.LENGTH_SHORT).show()
            return
        }

        resendButton.isEnabled = false

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setForceResendingToken(token)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    resendButton.isEnabled = true
                    Toast.makeText(this@OtpVerifyActivity, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    newToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = newVerificationId
                    PhoneAuthHolder.resendToken = newToken
                    Toast.makeText(this@OtpVerifyActivity, getString(R.string.otp_resent_message), Toast.LENGTH_SHORT).show()
                    startResendTimer()
                }
            })

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun startResendTimer() {
        resendButton.isEnabled = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                resendButton.text = getString(R.string.resend_otp_in_seconds, secondsLeft)
            }

            override fun onFinish() {
                resendButton.isEnabled = true
                resendButton.text = getString(R.string.resend_otp_button)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
