package com.ahmed.blooddonation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HospitalDonorOfferAdapter(
    private val offers: List<DonorOffer>,
    private val onOfferUpdated: () -> Unit
) : RecyclerView.Adapter<HospitalDonorOfferAdapter.OfferViewHolder>() {

    class OfferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val donorNameText: TextView = view.findViewById(R.id.hospitalOfferDonorNameText)
        val donorInfoText: TextView = view.findViewById(R.id.hospitalOfferDonorInfoText)
        val dateText: TextView = view.findViewById(R.id.hospitalOfferDateText)
        val statusText: TextView = view.findViewById(R.id.hospitalOfferStatusText)
        val confirmButton: Button = view.findViewById(R.id.confirmReceivedButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital_donor_offer, parent, false)
        return OfferViewHolder(view)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context

        holder.donorNameText.text = offer.donorName
        holder.donorInfoText.text = context.getString(
            R.string.hospital_offer_info_line,
            offer.bloodType,
            offer.donorPhone,
            offer.city
        )

        val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("ar"))
        val dateStr = dateFormat.format(Date(offer.timestamp))
        holder.dateText.text = context.getString(R.string.offer_date_prefix, dateStr)

        if (offer.hospitalConfirmed || offer.donorConfirmed) {
            holder.statusText.text = context.getString(R.string.offer_status_completed)
            holder.confirmButton.visibility = View.GONE
        } else {
            holder.statusText.text = context.getString(R.string.offer_status_pending)
            holder.confirmButton.visibility = View.VISIBLE
        }

        holder.confirmButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection("donorOffers")
                .document(offer.id)
                .update(
                    mapOf(
                        "hospitalConfirmed" to true,
                        "completedTimestamp" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(context, context.getString(R.string.donation_marked_confirmed), Toast.LENGTH_SHORT).show()
                    onOfferUpdated()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, context.getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun getItemCount(): Int = offers.size
}
