package com.ahmed.blooddonation

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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

            val options = arrayOf("اتصال", "واتساب")
            AlertDialog.Builder(context)
                .setTitle("طريقة التواصل")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse("tel:${request.contactPhone}")
                            context.startActivity(intent)
                        }
                        1 -> {
                            val phone = formatPhoneForWhatsApp(request.contactPhone)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("https://wa.me/$phone")
                            context.startActivity(intent)
                        }
                    }
                }
                .show()
        }
    }

    private fun formatPhoneForWhatsApp(phone: String): String {
        var cleaned = phone.trim().replace(" ", "").replace("-", "")
        if (cleaned.startsWith("0")) {
            cleaned = "966" + cleaned.substring(1)
        } else if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1)
        } else if (!cleaned.startsWith("966")) {
            cleaned = "966$cleaned"
        }
        return cleaned
    }

    override fun getItemCount(): Int = requests.size
    }
