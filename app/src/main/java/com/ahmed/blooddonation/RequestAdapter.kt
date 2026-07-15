package com.ahmed.blooddonation

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RequestAdapter(private val requests: List<Request>) :
    RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bloodType: TextView = view.findViewById(R.id.bloodTypeText)
        val urgency: TextView = view.findViewById(R.id.urgencyText)
        val city: TextView = view.findViewById(R.id.cityText)
        val requesterName: TextView = view.findViewById(R.id.requesterNameText)
        val notes: TextView = view.findViewById(R.id.notesText)
        val contactButton: Button = view.findViewById(R.id.contactButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.bloodType.text = request.bloodType
        holder.urgency.text = request.urgency
        holder.city.text = "المدينة: ${request.city}"
        holder.requesterName.text = "بواسطة: ${request.requesterName}"
        holder.notes.text = request.notes

        holder.contactButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:${request.contactPhone}")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = requests.size
    }
