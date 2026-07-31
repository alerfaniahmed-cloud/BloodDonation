package com.ahmed.blooddonation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DonationHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.historyRecyclerView)
        emptyText = findViewById(R.id.emptyHistoryText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("donorOffers")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { result ->
                val completedOffers = mutableListOf<DonorOffer>()
                for (doc in result) {
                    val offer = doc.toObject(DonorOffer::class.java)
                    offer.id = doc.id
                    if (offer.donorConfirmed || offer.hospitalConfirmed) {
                        completedOffers.add(offer)
                    }
                }

                val sorted = completedOffers.sortedByDescending {
                    if (it.completedTimestamp > 0L) it.completedTimestamp else it.timestamp
                }

                if (sorted.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = DonationHistoryAdapter(sorted)
                }
            }
    }
}
