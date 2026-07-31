package com.ahmed.blooddonation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmergencyCardActivity : AppCompatActivity() {

    private var contactPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_card)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        val bloodTypeText = findViewById<TextView>(R.id.emergencyBloodTypeText)
        val nameText = findViewById<TextView>(R.id.emergencyNameText)
        val phoneText = findViewById<TextView>(R.id.emergencyPhoneText)
        val cityText = findViewById<TextView>(R.id.emergencyCityText)
        val callButton = findViewById<Button>(R.id.callEmergencyContactButton)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val bloodType = doc.getString("bloodType") ?: "--"
                    val name = doc.getString("name") ?: getString(R.string.default_requester_name)
                    val phone = doc.getString("phone") ?: ""
                    val city = doc.getString("city") ?: ""

                    bloodTypeText.text = bloodType
                    nameText.text = name
                    phoneText.text = phone
                    cityText.text = city
                    contactPhone = phone
                }
        }

        callButton.setOnClickListener {
            if (contactPhone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$contactPhone")
                startActivity(intent)
            }
        }
    }
}
