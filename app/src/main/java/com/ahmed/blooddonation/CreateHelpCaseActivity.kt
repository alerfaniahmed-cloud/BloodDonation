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

class CreateHelpCaseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var editingCaseId: String? = null
    private var existingTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_help_case)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val titleInput = findViewById<EditText>(R.id.caseTitleInput)
        val descriptionInput = findViewById<EditText>(R.id.caseDescriptionInput)
        val categorySpinner = findViewById<Spinner>(R.id.caseCategorySpinner)
        val cityInput = findViewById<EditText>(R.id.caseCityInput)
        val amountInput = findViewById<EditText>(R.id.caseAmountInput)
        val charityNameInput = findViewById<EditText>(R.id.charityNameInput)
        val charityContactInput = findViewById<EditText>(R.id.charityContactInput)
        val submitButton = findViewById<Button>(R.id.submitHelpCaseButton)

        val categories = resources.getStringArray(R.array.help_case_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = adapter

        editingCaseId = intent.getStringExtra("caseId")
        existingTimestamp = intent.getLongExtra("timestamp", 0L)

        if (editingCaseId != null) {
            titleInput.setText(intent.getStringExtra("title"))
            descriptionInput.setText(intent.getStringExtra("description"))
            cityInput.setText(intent.getStringExtra("city"))
            amountInput.setText(intent.getStringExtra("amountNeeded"))
            charityNameInput.setText(intent.getStringExtra("charityName"))
            charityContactInput.setText(intent.getStringExtra("charityContact"))
            submitButton.text = getString(R.string.update_help_case_button)

            val categoryExtra = intent.getStringExtra("category")
            val categoryIndex = categories.indexOf(categoryExtra)
            if (categoryIndex >= 0) categorySpinner.setSelection(categoryIndex)
        }

        submitButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()
            val city = cityInput.text.toString().trim()
            val amount = amountInput.text.toString().trim()
            val charityName = charityNameInput.text.toString().trim()
            val charityContact = charityContactInput.text.toString().trim()
            val category = categorySpinner.selectedItem.toString()

            if (title.isEmpty() || description.isEmpty() || charityName.isEmpty() || charityContact.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_required_help_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: ""

            db.collection("users").document(userId).get()
                .addOnSuccessListener { userDoc ->
                    val requesterName = userDoc.getString("name") ?: getString(R.string.default_requester_name)

                    val helpCase = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "category" to category,
                        "amountNeeded" to amount,
                        "city" to city,
                        "charityName" to charityName,
                        "charityContact" to charityContact,
                        "requesterName" to requesterName,
                        "userId" to userId,
                        "timestamp" to if (editingCaseId != null) existingTimestamp else System.currentTimeMillis()
                    )

                    if (editingCaseId != null) {
                        db.collection("helpCases").document(editingCaseId!!)
                            .set(helpCase)
                            .addOnSuccessListener {
                                Toast.makeText(this, getString(R.string.help_case_published), Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                            }
                    } else {
                        db.collection("helpCases")
                            .add(helpCase)
                            .addOnSuccessListener {
                                Toast.makeText(this, getString(R.string.help_case_published), Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                            }
                    }
                }
        }
    }
}
