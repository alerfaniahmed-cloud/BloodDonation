package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val cityInput = findViewById<EditText>(R.id.cityInput)
        val bloodTypeSpinner = findViewById<Spinner>(R.id.bloodTypeSpinner)
        val saveButton = findViewById<Button>(R.id.saveProfileButton)

        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypes)
        bloodTypeSpinner.adapter = adapter

        loadExistingProfile(nameInput, phoneInput, cityInput, bloodTypeSpinner, bloodTypes)

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val bloodType = bloodTypeSpinner.selectedItem.toString()

            if (name.isEmpty() || phone.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, "الرجاء تعبئة جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "حدث خطأ، الرجاء تسجيل الدخول مرة أخرى", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userProfile = hashMapOf(
                "name" to name,
                "phone" to phone,
                "city" to city,
                "bloodType" to bloodType,
                "email" to auth.currentUser?.email
            )

            db.collection("users").document(userId)
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "تم حفظ بياناتك بنجاح", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadExistingProfile(
        nameInput: EditText,
        phoneInput: EditText,
        cityInput: EditText,
        bloodTypeSpinner: Spinner,
        bloodTypes: Array<String>
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    nameInput.setText(doc.getString("name") ?: "")
                    phoneInput.setText(doc.getString("phone") ?: "")
                    cityInput.setText(doc.getString("city") ?: "")
                    val savedBloodType = doc.getString("bloodType")
                    val index = bloodTypes.indexOf(savedBloodType)
                    if (index >= 0) {
                        bloodTypeSpinner.setSelection(index)
                    }
                }
            }
    }
}
