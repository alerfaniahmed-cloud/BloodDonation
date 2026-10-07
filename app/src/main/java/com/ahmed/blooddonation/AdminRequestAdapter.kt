package com.ahmed.blooddonation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminRequestAdapter(
    private val requests: List<Request>,
    private val onDeleteClick: (Request) -> Unit
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bloodTypeText: TextView = view.findViewById(R.id.adminBloodTypeText)
        val urgencyText: TextView = view.findViewById(R.id.adminUrgencyText)
        val cityText: TextView = view.findViewById(R.id.adminCityText)
        val requesterText: TextView = view.findViewById(R.id.adminRequesterText)
        val phoneText: TextView = view.findViewById(R.id.adminPhoneText)
        val dateText: TextView = view.findViewById(R.id.adminDateText)
        val deleteButton: Button = view.findViewById(R.id.adminDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        val context = holder.itemView.context

        holder.bloodTypeText.text = request.bloodType
        holder.urgencyText.text = request.urgency
        holder.cityText.text = context.getString(R.string.admin_city_prefix, request.city)
        holder.requesterText.text = context.getString(R.string.admin_requester_prefix, request.requesterName)
        holder.phoneText.text = context.getString(R.string.admin_phone_prefix, request.contactPhone)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(request.timestamp))

        holder.deleteButton.setOnClickListener {
            onDeleteClick(request)
        }
    }

    override fun getItemCount(): Int = requests.size
}
