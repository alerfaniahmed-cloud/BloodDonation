package com.ahmed.blooddonation

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class HospitalAdapter(private val hospitals: List<Hospital>) :
    RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder>() {

    class HospitalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.hospitalNameText)
        val city: TextView = view.findViewById(R.id.hospitalCityText)
        val contactButton: Button = view.findViewById(R.id.hospitalContactButton)
        val locationButton: Button = view.findViewById(R.id.hospitalLocationButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HospitalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hospital, parent, false)
        return HospitalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HospitalViewHolder, position: Int) {
        val hospital = hospitals[position]
        holder.name.text = hospital.name
        holder.city.text = hospital.city

        holder.contactButton.setOnClickListener {
            val context = holder.itemView.context
            val options = arrayOf(
                context.getString(R.string.call_option),
                context.getString(R.string.whatsapp_option)
            )
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.contact_method_title))
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse("tel:${hospital.phone}")
                            context.startActivity(intent)
                        }
                        1 -> {
                            val phone = formatPhoneForWhatsApp(hospital.phone)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("https://wa.me/$phone")
                            context.startActivity(intent)
                        }
                    }
                }
                .show()
        }

        holder.locationButton.setOnClickListener {
            val context = holder.itemView.context
            val label = Uri.encode(hospital.name)
            val geoUri = "geo:${hospital.lat},${hospital.lng}?q=${hospital.lat},${hospital.lng}($label)"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = "https://www.google.com/maps/search/?api=1&query=${hospital.lat},${hospital.lng}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri))
                context.startActivity(webIntent)
            }
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

    override fun getItemCount(): Int = hospitals.size
    }
