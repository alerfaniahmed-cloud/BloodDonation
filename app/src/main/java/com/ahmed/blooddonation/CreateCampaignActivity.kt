package com.ahmed.blooddonation

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateCampaignActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_campaign)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val titleInput = findViewById<EditText>(R.id.campaignTitleInput)
        val dateTimeInput = findViewById<EditText>(R.id.campaignDateTimeInput)
        val cityInput = findViewById<EditText>(R.id.campaignCityInput)
        val notesInput = findViewById<EditText>(R.id.campaignNotesInput)
        val publishButton = findViewById<Button>(R.id.publishCampaignButton)

        publishButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val dateTime = dateTimeInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (title.isEmpty() || dateTime.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_required_campaign_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, getString(R.string.error_relogin), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val hospitalName = doc.getString("name") ?: getString(R.string.registered_hospital_default)

                    val campaign = hashMapOf(
                        "hospitalId" to userId,
                        "hospitalName" to hospitalName,
                        "title" to title,
                        "dateTimeText" to dateTime,
                        "city" to city,
                        "notes" to notes,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("campaigns").add(campaign)
                        .addOnSuccessListener {
                            Toast.makeText(this, getString(R.string.campaign_published), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                        }
                }
        }
    }
}
