package com.ahmed.blooddonation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var nameInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var cityInput: EditText
    private lateinit var bloodTypeSpinner: Spinner
    private lateinit var captureLocationButton: Button
    private lateinit var locationStatusText: TextView
    private lateinit var donorBadgeCard: CardView
    private lateinit var donationCountText: TextView
    private lateinit var livesSavedText: TextView

    private var accountType: String = "individual"
    private var hospitalLat: Double? = null
    private var hospitalLng: Double? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 200
        private const val LIVES_PER_DONATION = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        nameInput = findViewById(R.id.nameInput)
        phoneInput = findViewById(R.id.phoneInput)
        cityInput = findViewById(R.id.cityInput)
        bloodTypeSpinner = findViewById(R.id.bloodTypeSpinner)
        captureLocationButton = findViewById(R.id.captureLocationButton)
        locationStatusText = findViewById(R.id.locationStatusText)
        donorBadgeCard = findViewById(R.id.donorBadgeCard)
        donationCountText = findViewById(R.id.donationCountText)
        livesSavedText = findViewById(R.id.livesSavedText)
        val saveButton = findViewById<Button>(R.id.saveProfileButton)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bloodTypes)
        bloodTypeSpinner.adapter = adapter

        loadExistingProfile(bloodTypes)

        captureLocationButton.setOnClickListener {
            requestLocationAndCapture()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val city = cityInput.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || city.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, getString(R.string.error_relogin), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userProfile = hashMapOf<String, Any>(
                "name" to name,
                "phone" to phone,
                "city" to city,
                "email" to (auth.currentUser?.email ?: "")
            )

            if (accountType == "hospital") {
                if (hospitalLat != null && hospitalLng != null) {
                    userProfile["lat"] = hospitalLat!!
                    userProfile["lng"] = hospitalLng!!
                }
            } else {
                val bloodType = bloodTypeSpinner.selectedItem.toString()
                userProfile["bloodType"] = bloodType
            }

            db.collection("users").document(userId)
                .set(userProfile, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.profile_saved), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }
        }

        logoutButton.setOnClickListener {
            performLogout()
        }

        // بدل ما زر الرجوع بالجهاز يطلع من التطبيق، يرجعنا للشاشة الرئيسية
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                finish()
            }
        })
    }

    private fun performLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadExistingProfile(bloodTypes: Array<String>) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    accountType = doc.getString("accountType") ?: "individual"
                    applyAccountTypeUI()

                    nameInput.setText(doc.getString("name") ?: "")
                    phoneInput.setText(doc.getString("phone") ?: "")
                    cityInput.setText(doc.getString("city") ?: "")

                    if (accountType == "hospital") {
                        val lat = doc.getDouble("lat")
                        val lng = doc.getDouble("lng")
                        if (lat != null && lng != null) {
                            hospitalLat = lat
                            hospitalLng = lng
                            locationStatusText.text = getString(R.string.location_saved_prefix, lat.toString(), lng.toString())
                        }
                    } else {
                        val savedBloodType = doc.getString("bloodType")
                        val index = bloodTypes.indexOf(savedBloodType)
                        if (index >= 0) {
                            bloodTypeSpinner.setSelection(index)
                        }
                        loadDonationBadge(userId)
                    }
                } else {
                    applyAccountTypeUI()
                }
            }
            .addOnFailureListener {
                applyAccountTypeUI()
            }
    }

    private fun loadDonationBadge(userId: String) {
        db.collection("donorOffers")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { result ->
                var completedCount = 0
                for (doc in result) {
                    val donorConfirmed = doc.getBoolean("donorConfirmed") ?: false
                    val hospitalConfirmed = doc.getBoolean("hospitalConfirmed") ?: false
                    if (donorConfirmed || hospitalConfirmed) {
                        completedCount++
                    }
                }

                if (completedCount > 0) {
                    donorBadgeCard.visibility = View.VISIBLE
                    donationCountText.text = getString(R.string.donation_count_value, completedCount)
                    val livesSaved = completedCount * LIVES_PER_DONATION
                    livesSavedText.text = getString(R.string.lives_saved_text, livesSaved)
                } else {
                    donorBadgeCard.visibility = View.GONE
                }
            }
    }

    private fun applyAccountTypeUI() {
        if (accountType == "hospital") {
            bloodTypeSpinner.visibility = View.GONE
            captureLocationButton.visibility = View.VISIBLE
            locationStatusText.visibility = View.VISIBLE
            donorBadgeCard.visibility = View.GONE
        } else {
            bloodTypeSpinner.visibility = View.VISIBLE
            captureLocationButton.visibility = View.GONE
            locationStatusText.visibility = View.GONE
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
                hospitalLat = bestLocation.latitude
                hospitalLng = bestLocation.longitude
                locationStatusText.text = getString(
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
