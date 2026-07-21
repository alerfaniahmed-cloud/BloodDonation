package com.ahmed.blooddonation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateRequestActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var editRequestId: String? = null
    private var capturedLat: Double? = null
    private var capturedLng: Double? = null
    private lateinit var attachLocationStatusText: TextView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 500
    }

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
        val attachLocationButton = findViewById<Button>(R.id.attachLocationButton)
        attachLocationStatusText = findViewById(R.id.attachLocationStatusText)

        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypes)
        bloodTypeSpinner.adapter = bloodAdapter

        val urgencyLevels = resources.getStringArray(R.array.urgency_levels)
        val urgencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, urgencyLevels)
        urgencySpinner.adapter = urgencyAdapter

        editRequestId = intent.getStringExtra("editRequestId")
        val isEmergency = intent.getBooleanExtra("isEmergency", false)

        if (editRequestId != null) {
            submitButton.text = getString(R.string.update_request_button)

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
        } else if (isEmergency && urgencyLevels.isNotEmpty()) {
            urgencySpinner.setSelection(0)
        }

        attachLocationButton.setOnClickListener {
            requestLocationAndCapture()
        }

        submitButton.setOnClickListener {
            val city = cityInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()
            val bloodType = bloodTypeSpinner.selectedItem.toString()
            val urgency = urgencySpinner.selectedItem.toString()

            if (city.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_city_phone), Toast.LENGTH_SHORT).show()
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
                if (capturedLat != null && capturedLng != null) {
                    updates["lat"] = capturedLat!!
                    updates["lng"] = capturedLng!!
                }

                db.collection("requests").document(editRequestId!!)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.request_updated), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                    }
            } else {
                val userId = auth.currentUser?.uid
                db.collection("users").document(userId ?: "").get()
                    .addOnSuccessListener { userDoc ->
                        val requesterName = userDoc.getString("name") ?: getString(R.string.default_requester_name)
                        val requesterType = userDoc.getString("accountType") ?: "individual"

                        val request = hashMapOf(
                            "bloodType" to bloodType,
                            "urgency" to urgency,
                            "city" to city,
                            "contactPhone" to phone,
                            "notes" to notes,
                            "requesterName" to requesterName,
                            "timestamp" to System.currentTimeMillis(),
                            "userId" to (userId ?: ""),
                            "requesterType" to requesterType
                        )
                        if (capturedLat != null && capturedLng != null) {
                            request["lat"] = capturedLat!!
                            request["lng"] = capturedLng!!
                        }

                        db.collection("requests")
                            .add(request)
                            .addOnSuccessListener {
                                Toast.makeText(this, getString(R.string.request_published), Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
    }

    private fun requestLocationAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }
        captureCurrentLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureCurrentLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureCurrentLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)

            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                    bestLocation = location
                }
            }

            if (bestLocation != null) {
                capturedLat = bestLocation.latitude
                capturedLng = bestLocation.longitude
                attachLocationStatusText.visibility = android.view.View.VISIBLE
                attachLocationStatusText.text = getString(
                    R.string.location_captured_prefix,
                    bestLocation.latitude.toString(),
                    bestLocation.longitude.toString()
                )
                Toast.makeText(this, getString(R.string.location_saved_current), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.location_capture_failed), Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, getString(R.string.location_permission_error), Toast.LENGTH_SHORT).show()
        }
    }
}
