package com.ahmed.blooddonation

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateRequestActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var editRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_request)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val bloodTypeSpinner = findViewById<Spinner>(R.id.bloodTypeSpinner)
        val urgencySpinner = findViewById<Spinner>(R.id.urgencySpinner)
        val cityInput = findViewById<EditText>(R.id.cityInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val notesInput = findViewById<EditText>(R.id.notesInput)
        val submitButton = findViewById<Button>(R.id.submitButton)

        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypes)
        bloodTypeSpinner.adapter = bloodAdapter

        val urgencyLevels = arrayOf("عاجل", "خلال يومين", "غير عاجل")
        val urgencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, urgencyLevels)
        urgencySpinner.adapter = urgencyAdapter

        editRequestId = intent.getStringExtra("editRequestId")

        if (editRequestId != null) {
            submitButton.text = "تحديث النداء"

            val editBloodType = intent.getStringExtra("editBloodType")
            val editUrgency = intent.getStringExtra("editUrgency")
            val editCity = intent.getStringExtra("editCity")
            val editPhone = intent.getStringExtra("editPhone")
            val editNotes = intent.getStringExtra("editNotes")

            val bloodIndex = bloodTypes.indexOf(editBloodType)
            if (bloodIndex >= 0) bloodTypeSpinner.setSelection(bloodIndex)

            val urgencyIndex = urgencyLevels.indexOf(editUrgency)
            if (urgencyIndex >= 0) urgencySpinner.setSelection(urgencyIndex)

            cityInput.setText(editCity ?: "")
            phoneInput.setText(editPhone ?: "")
            notesInput.setText(editNotes ?: "")
        }

        submitButton.setOnClickListener {
            val city = cityInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()
            val bloodType = bloodTypeSpinner.selectedItem.toString()
            val urgency = urgencySpinner.selectedItem.toString()

            if (city.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "الرجاء تعبئة المدينة ورقم التواصل", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (editRequestId != null) {
                val updates = hashMapOf<String, Any>(
                    "bloodType" to bloodType,
                    "urgency" to urgency,
                    "city" to city,
                    "contactPhone" to phone,
                    "notes" to notes
                )

                db.collection("requests").document(editRequestId!!)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "تم تحديث النداء بنجاح", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                val userId = auth.currentUser?.uid
                db.collection("users").document(userId ?: "").get()
                    .addOnSuccessListener { userDoc ->
                        val requesterName = userDoc.getString("name") ?: "مستخدم"

                        val request = hashMapOf(
                            "bloodType" to bloodType,
                            "urgency" to urgency,
                            "city" to city,
                            "contactPhone" to phone,
                            "notes" to notes,
                            "requesterName" to requesterName,
                            "timestamp" to System.currentTimeMillis(),
                            "userId" to (userId ?: "")
                        )

                        db.collection("requests")
                            .add(request)
                            .addOnSuccessListener {
                                Toast.makeText(this, "تم نشر النداء بنجاح", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
    }
}
