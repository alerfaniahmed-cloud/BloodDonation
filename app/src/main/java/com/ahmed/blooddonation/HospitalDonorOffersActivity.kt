package com.ahmed.blooddonation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HospitalDonorOffersActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital_donor_offers)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.hospitalOffersRecyclerView)
        emptyText = findViewById(R.id.emptyHospitalOffersText)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        loadOffers()
    }

    private fun loadOffers() {
        val hospitalId = auth.currentUser?.uid ?: return

        db.collection("donorOffers")
            .whereEqualTo("targetHospitalId", hospitalId)
            .get()
            .addOnSuccessListener { result ->
                val offers = mutableListOf<DonorOffer>()
                for (doc in result) {
                    val offer = doc.toObject(DonorOffer::class.java)
                    offer.id = doc.id
                    offers.add(offer)
                }

                val sortedOffers = offers.sortedByDescending { it.timestamp }

                if (sortedOffers.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = HospitalDonorOfferAdapter(sortedOffers) {
                        loadOffers()
                    }
                }
            }
    }
}
