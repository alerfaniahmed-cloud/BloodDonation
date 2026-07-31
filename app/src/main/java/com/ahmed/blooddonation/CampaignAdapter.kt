package com.ahmed.blooddonation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CampaignAdapter(
    private val campaigns: List<Campaign>,
    private val onRegistered: () -> Unit
) : RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {

    class CampaignViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.campaignTitleText)
        val hospitalText: TextView = view.findViewById(R.id.campaignHospitalText)
        val dateTimeText: TextView = view.findViewById(R.id.campaignDateTimeText)
        val cityText: TextView = view.findViewById(R.id.campaignCityText)
        val notesText: TextView = view.findViewById(R.id.campaignNotesText)
        val countText: TextView = view.findViewById(R.id.campaignCountText)
        val registerButton: Button = view.findViewById(R.id.campaignRegisterButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_campaign, parent, false)
        return CampaignViewHolder(view)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        val campaign = campaigns[position]
        val context = holder.itemView.context

        holder.titleText.text = campaign.title
        holder.hospitalText.text = context.getString(R.string.campaign_hospital_prefix, campaign.hospitalName)
        holder.dateTimeText.text = campaign.dateTimeText
        holder.cityText.text = campaign.city

        if (campaign.notes.isNotBlank()) {
            holder.notesText.visibility = View.VISIBLE
            holder.notesText.text = campaign.notes
        } else {
            holder.notesText.visibility = View.GONE
        }

        holder.countText.text = context.getString(R.string.campaign_registered_count, campaign.registeredCount)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId == null || currentUserId == campaign.hospitalId) {
            holder.registerButton.visibility = View.GONE
        } else {
            holder.registerButton.visibility = View.VISIBLE
            holder.registerButton.isEnabled = true
            holder.registerButton.text = context.getString(R.string.campaign_register_button)

            checkIfAlreadyRegistered(campaign.id, currentUserId, holder.registerButton, context)

            holder.registerButton.setOnClickListener {
                registerForCampaign(campaign, currentUserId, holder.registerButton, context)
            }
        }
    }

    private fun checkIfAlreadyRegistered(campaignId: String, userId: String, button: Button, context: android.content.Context) {
        FirebaseFirestore.getInstance().collection("campaigns")
            .document(campaignId)
            .collection("registrations")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    button.isEnabled = false
                    button.text = context.getString(R.string.campaign_already_registered)
                }
            }
    }

    private fun registerForCampaign(campaign: Campaign, userId: String, button: Button, context: android.content.Context) {
        val db = FirebaseFirestore.getInstance()
        val campaignRef = db.collection("campaigns").document(campaign.id)
        val registrationRef = campaignRef.collection("registrations").document(userId)

        registrationRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                button.isEnabled = false
                button.text = context.getString(R.string.campaign_already_registered)
                return@addOnSuccessListener
            }

            registrationRef.set(mapOf("timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    val newCount = campaign.registeredCount + 1
                    campaignRef.update("registeredCount", newCount)
                        .addOnSuccessListener {
                            button.isEnabled = false
                            button.text = context.getString(R.string.campaign_already_registered)
                            Toast.makeText(context, context.getString(R.string.campaign_registered_toast), Toast.LENGTH_SHORT).show()
                            onRegistered()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, context.getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun getItemCount(): Int = campaigns.size
}
