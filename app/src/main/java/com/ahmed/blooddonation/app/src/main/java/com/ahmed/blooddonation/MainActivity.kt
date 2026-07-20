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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var hospitalOffersButton: Button

    private var allRequests: List<Request> = listOf()
    private var requestListener: ListenerRegistration? = null
    private var donorOfferListener: ListenerRegistration? = null
    private var screenOpenTime: Long = 0L

    private var currentUserBloodType: String? = null
    private var currentUserCity: String? = null
    private var currentAccountType: String = "individual"

    companion object {
        private const val CHANNEL_ID = "blood_requests_channel"
        private const val DONOR_OFFER_CHANNEL_ID = "donor_offer_channel"
        private const val ELIGIBILITY_CHANNEL_ID = "donation_eligibility_channel"
        private const val NOTIFICATION_PERMISSION_REQUEST = 300
        private const val ELIGIBILITY_DAYS = 56
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        recyclerView = findViewById(R.id.requestsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        bloodTypeFilterSpinner = findViewById(R.id.bloodTypeFilterSpinner)
        cityFilterInput = findViewById(R.id.cityFilterInput)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        hospitalOffersButton = findViewById(R.id.hospitalOffersButton)
        val addButton = findViewById<Button>(R.id.addButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val hospitalsButton = findViewById<Button>(R.id.hospitalsButton)
        val helpCasesButton = findViewById<Button>(R.id.helpCasesButton)
        val myOffersButton = findViewById<Button>(R.id.myOffersButton)
        val languageButton = findViewById<Button>(R.id.languageButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val bloodTypes = resources.getStringArray(R.array.blood_types_filter)
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

        swipeRefreshLayout.setOnRefreshListener {
            loadRequests()
        }

        updateLanguageButtonText(languageButton)
        languageButton.setOnClickListener {
            toggleAppLanguage()
        }

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

        helpCasesButton.setOnClickListener {
            startActivity(Intent(this, HelpCasesActivity::class.java))
        }

        myOffersButton.setOnClickListener {
            startActivity(Intent(this, MyDonorOffersActivity::class.java))
        }

        hospitalOffersButton.setOnClickListener {
            startActivity(Intent(this, HospitalDonorOffersActivity::class.java))
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
        donorOfferListener?.remove()
        donorOfferListener = null
    }

    private fun loadCurrentUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    currentAccountType = doc.getString("accountType") ?: "individual"
                    currentUserBloodType = doc.getString("bloodType")
                    currentUserCity = doc.getString("city")

                    if (currentAccountType == "hospital") {
                        hospitalOffersButton.visibility = View.VISIBLE
                        startDonorOfferListener(userId)
                    } else {
                        hospitalOffersButton.visibility = View.GONE
                        checkDonationEligibilityReminder(userId)
                    }
                }
            }
    }

    private fun checkDonationEligibilityReminder(userId: String) {
        db.collection("donorOffers")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { result ->
                var lastCompletedTimestamp = 0L
                for (doc in result) {
                    val donorConfirmed = doc.getBoolean("donorConfirmed") ?: false
                    val hospitalConfirmed = doc.getBoolean("hospitalConfirmed") ?: false
                    if (donorConfirmed || hospitalConfirmed) {
                        val completedTimestamp = doc.getLong("completedTimestamp") ?: 0L
                        if (completedTimestamp > lastCompletedTimestamp) {
                            lastCompletedTimestamp = completedTimestamp
                        }
                    }
                }

                if (lastCompletedTimestamp <= 0L) return@addOnSuccessListener

                val eligibleDate = lastCompletedTimestamp + (ELIGIBILITY_DAYS * MILLIS_PER_DAY)
                if (System.currentTimeMillis() < eligibleDate) return@addOnSuccessListener

                db.collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val alreadyNotifiedFor = userDoc.getLong("eligibilityNotifiedFor") ?: 0L
                        if (alreadyNotifiedFor == lastCompletedTimestamp) return@addOnSuccessListener

                        showEligibilityNotification()
                        db.collection("users").document(userId)
                            .update("eligibilityNotifiedFor", lastCompletedTimestamp)
                    }
            }
    }

    private fun showEligibilityNotification() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasPermission) return

        val intent = Intent(this, ProfileActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, ELIGIBILITY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.eligibility_notification_title))
            .setContentText(getString(R.string.eligibility_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(9001, builder.build())
    }

    private fun loadRequests() {
        swipeRefreshLayout.isRefreshing = true
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
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false
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

    private fun startDonorOfferListener(hospitalUserId: String) {
        donorOfferListener = db.collection("donorOffers")
            .whereEqualTo("targetHospitalId", hospitalUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val offer = change.document.toObject(DonorOffer::class.java)
                        offer.id = change.document.id

                        if (offer.timestamp > screenOpenTime) {
                            showDonorOfferNotification(offer)
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

    private fun showDonorOfferNotification(offer: DonorOffer) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasPermission) return

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, DONOR_OFFER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.donor_offer_notification_title))
            .setContentText(getString(R.string.donor_offer_notification_text, offer.donorName, offer.bloodType, offer.city))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = offer.id.hashCode()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val requestsChannel = NotificationChannel(
                CHANNEL_ID,
                "طلبات التبرع بالدم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عند نشر طلب دم يطابق فصيلتك ومدينتك"
            }
            manager.createNotificationChannel(requestsChannel)

            val donorOfferChannel = NotificationChannel(
                DONOR_OFFER_CHANNEL_ID,
                "متبرعون جاهزون",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات عند وجود متبرع جاهز بالقرب من مستشفاك"
            }
            manager.createNotificationChannel(donorOfferChannel)

            val eligibilityChannel = NotificationChannel(
                ELIGIBILITY_CHANNEL_ID,
                "تذكير التبرع",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعار عند اكتمال 56 يوم من آخر تبرع"
            }
            manager.createNotificationChannel(eligibilityChannel)
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
        val selectedPosition = bloodTypeFilterSpinner.selectedItemPosition
        val selectedBloodType = bloodTypeFilterSpinner.selectedItem?.toString() ?: ""
        val cityQuery = cityFilterInput.text.toString().trim()

        val filtered = allRequests.filter { request ->
            val matchesBloodType = selectedPosition == 0 || request.bloodType == selectedBloodType
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
