package com.ahmed.blooddonation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DonorLeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
    RecyclerView.Adapter<DonorLeaderboardAdapter.EntryViewHolder>() {

    class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(R.id.rankText)
        val nameText: TextView = view.findViewById(R.id.donorNameText)
        val countText: TextView = view.findViewById(R.id.donationCountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_entry, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        holder.rankText.text = when (position) {
            0 -> "🥇"
            1 -> "🥈"
            2 -> "🥉"
            else -> (position + 1).toString()
        }

        holder.nameText.text = entry.donorName
        holder.countText.text = context.getString(R.string.leaderboard_donation_count, entry.donationCount)
    }

    override fun getItemCount(): Int = entries.size
    }
