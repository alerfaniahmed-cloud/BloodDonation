package com.ahmed.blooddonation

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CircleMemberAdapter(private val members: List<CircleMemberInfo>) :
    RecyclerView.Adapter<CircleMemberAdapter.MemberViewHolder>() {

    class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.memberNameText)
        val bloodTypeText: TextView = view.findViewById(R.id.memberBloodTypeText)
        val cityText: TextView = view.findViewById(R.id.memberCityText)
        val callButton: Button = view.findViewById(R.id.memberCallButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_circle_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.nameText.text = member.name
        holder.bloodTypeText.text = member.bloodType.ifBlank { "-" }
        holder.cityText.text = member.city

        holder.callButton.setOnClickListener {
            if (member.phone.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${member.phone}")
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = members.size
    }
