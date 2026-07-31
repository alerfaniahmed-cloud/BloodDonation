package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampaignsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var createCampaignButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campaigns)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.campaignsRecyclerView)
        emptyText = findViewById(R.id.emptyCampaignsText)
        createCampaignButton = findViewById(R.id.createCampaignButton)
        recyclerView.layoutManager = LinearLayoutManager(this)

        createCampaignButton.setOnClickListener {
            startActivity(Intent(this, CreateCampaignActivity::class.java))
        }

        checkAccountType()
    }

    override fun onResume() {
        super.onResume()
        loadCampaigns()
    }

    private fun checkAccountType() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val accountType = doc.getString("accountType") ?: "individual"
                createCampaignButton.visibility = if (accountType == "hospital") View.VISIBLE else View.GONE
            }
    }

    private fun loadCampaigns() {
        db.collection("campaigns")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val campaigns = mutableListOf<Campaign>()
                for (doc in result) {
                    val campaign = doc.toObject(Campaign::class.java)
                    campaign.id = doc.id
                    campaigns.add(campaign)
                }

                if (campaigns.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = CampaignAdapter(campaigns) {
                        loadCampaigns()
                    }
                }
            }
    }
}
