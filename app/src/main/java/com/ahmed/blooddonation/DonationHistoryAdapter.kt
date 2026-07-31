package com.ahmed.blooddonation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DonationHistoryAdapter(private val offers: List<DonorOffer>) :
    RecyclerView.Adapter<DonationHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberText: TextView = view.findViewById(R.id.historyNumberText)
        val hospitalText: TextView = view.findViewById(R.id.historyHospitalText)
        val dateText: TextView = view.findViewById(R.id.historyDateText)
        val prayerText: TextView = view.findViewById(R.id.historyPrayerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donation_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val offer = offers[position]
        val context = holder.itemView.context

        val donationNumber = offers.size - position
        holder.numberText.text = context.getString(R.string.history_donation_number, donationNumber)

        holder.hospitalText.text = context.getString(R.string.offer_target_hospital_prefix, offer.targetHospitalName)

        val timestampToShow = if (offer.completedTimestamp > 0L) offer.completedTimestamp else offer.timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("ar"))
        val dateStr = dateFormat.format(Date(timestampToShow))
        holder.dateText.text = context.getString(R.string.offer_date_prefix, dateStr)

        if (offer.prayerMessage.isNotBlank()) {
            holder.prayerText.visibility = View.VISIBLE
            holder.prayerText.text = context.getString(R.string.prayer_received_prefix, offer.prayerMessage)
        } else {
            holder.prayerText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = offers.size
}
