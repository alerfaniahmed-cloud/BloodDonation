package com.ahmed.blooddonation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var bloodTypeFilterSpinner: Spinner
    private lateinit var cityFilterInput: EditText

    private var allRequests: List<Request> = listOf()
    private var requestListener: ListenerRegistration? = null
    private var screenOpenTime: Long = 0L

    private var currentUserBloodType: String? = null
    private var currentUserCity: String? = null
    private var currentAccountType: String = "individual"

    private val bloodTypes = arrayOf("الكل", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    companion object {
        private const val CHANNEL_ID = "blood_requests_channel"
        private const val NOTIFICATION_PERMISSION_REQUEST = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.requestsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        bloodTypeFilterSpinner = findViewById(R.id.bloodTypeFilterSpinner)
        cityFilterInput = findViewById(R.id.cityFilterInput)
        val addButton = findViewById<Button>(R.id.addButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val hospitalsButton = findViewById<Button>(R.id.hospitalsButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bloodTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bloodTypeFilterSpinner.adapter = spinnerAdapter

        bloodTypeFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        cityFilterInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        createNotificationChannel()
        requestNotificationPermission()
        loadCurrentUserProfile()

        addButton.setOnClickListener {
            startActivity(Intent(this, CreateRequestActivity::class.java))
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        hospitalsButton.setOnClickListener {
            startActivity(Intent(this, HospitalsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        screenOpenTime = System.currentTimeMillis()
        loadRequests()
        startNewRequestListener()
    }

    override fun onPause() {
        super.onPause()
        requestListener?.remove()
        requestListener = null
    }

    private fun loadCurrentUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    currentAccountType = doc.getString("accountType") ?: "individual"
                    currentUserBloodType = doc.getString("bloodType")
                    currentUserCity = doc.getString("city")
                }
            }
    }

    private fun loadRequests() {
        db.collection("requests")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val requests = mutableListOf<Request>()
                for (doc in result) {
                    val request = doc.toObject(Request::class.java)
                    request.id = doc.id
                    requests.add(request)
                }
                allRequests = requests
                applyFilters()
            }
    }

    private fun startNewRequestListener() {
        requestListener = db.collection("requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val request = change.document.toObject(Request::class.java)
                        request.id = change.document.id

                        if (request.timestamp > screenOpenTime) {
                            checkAndNotify(request)
                        }
                    }
                }
            }
    }

    private fun checkAndNotify(request: Request) {
        if (currentAccountType == "hospital") return

        val currentUserId = auth.currentUser?.uid
        if (request.userId.isNotEmpty() && request.userId == currentUserId) return

        val bloodTypeMatches = currentUserBloodType != null && request.bloodType == currentUserBloodType
        val cityMatches = currentUserCity != null &&
                request.city.contains(currentUserCity!!, ignoreCase = true)

        if (bloodTypeMatches && cityMatches) {
            showNotification(request)
        }
    }

    private fun showNotification(request: Request) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasPermission) return

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("طلب دم يطابقك: ${request.bloodType}")
            .setContentText("${request.city} - ${request.urgency}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = request.id.hashCode()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "طلبات التبرع بالدم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عند نشر طلب دم يطابق فصيلتك ومدينتك"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun applyFilters() {
        val selectedBloodType = bloodTypeFilterSpinner.selectedItem?.toString() ?: "الكل"
        val cityQuery = cityFilterInput.text.toString().trim()

        val filtered = allRequests.filter { request ->
            val matchesBloodType = selectedBloodType == "الكل" || request.bloodType == selectedBloodType
            val matchesCity = cityQuery.isEmpty() || request.city.contains(cityQuery, ignoreCase = true)
            matchesBloodType && matchesCity
        }

        if (filtered.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = RequestAdapter(filtered)
        }
    }
}
