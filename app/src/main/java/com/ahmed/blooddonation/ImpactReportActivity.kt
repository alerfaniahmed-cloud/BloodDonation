package com.ahmed.blooddonation

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ImpactReportActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var donationCount = 0
    private var livesSaved = 0
    private var requestsPublished = 0
    private var donorName = ""

    companion object {
        private const val LIVES_PER_DONATION = 3
        private const val MILLIS_PER_YEAR = 365L * 24 * 60 * 60 * 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impact_report)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val donationCountText = findViewById<TextView>(R.id.reportDonationCountText)
        val livesSavedText = findViewById<TextView>(R.id.reportLivesSavedText)
        val requestsText = findViewById<TextView>(R.id.reportRequestsText)
        val shareButton = findViewById<Button>(R.id.shareImpactReportButton)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        findViewById<TextView>(R.id.reportYearText).text = getString(R.string.impact_report_year_label, currentYear)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            finish()
            return
        }

        val oneYearAgo = System.currentTimeMillis() - MILLIS_PER_YEAR

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                donorName = doc.getString("name") ?: getString(R.string.default_requester_name)
                loadStats(userId, oneYearAgo, donationCountText, livesSavedText, requestsText, shareButton, currentYear)
            }
    }

    private fun loadStats(
        userId: String,
        oneYearAgo: Long,
        donationCountText: TextView,
        livesSavedText: TextView,
        requestsText: TextView,
        shareButton: Button,
        year: Int
    ) {
        db.collection("donorOffers")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { result ->
                var count = 0
                for (doc in result) {
                    val donorConfirmed = doc.getBoolean("donorConfirmed") ?: false
                    val hospitalConfirmed = doc.getBoolean("hospitalConfirmed") ?: false
                    val completedTimestamp = doc.getLong("completedTimestamp") ?: 0L
                    if ((donorConfirmed || hospitalConfirmed) && completedTimestamp >= oneYearAgo) {
                        count++
                    }
                }
                donationCount = count
                livesSaved = count * LIVES_PER_DONATION

                donationCountText.text = donationCount.toString()
                livesSavedText.text = livesSaved.toString()

                db.collection("requests")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { requestsResult ->
                        var reqCount = 0
                        for (doc in requestsResult) {
                            val timestamp = doc.getLong("timestamp") ?: 0L
                            if (timestamp >= oneYearAgo) {
                                reqCount++
                            }
                        }
                        requestsPublished = reqCount
                        requestsText.text = requestsPublished.toString()

                        shareButton.setOnClickListener {
                            ImpactReportGenerator.generateAndShare(
                                this, donorName, donationCount, livesSaved, requestsPublished, year
                            )
                        }
                    }
            }
    }
}
