package com.ahmed.blooddonation

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HelpCaseAdapter(private val cases: List<HelpCase>) :
    RecyclerView.Adapter<HelpCaseAdapter.HelpCaseViewHolder>() {

    class HelpCaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.caseTitleText)
        val category: TextView = view.findViewById(R.id.caseCategoryText)
        val city: TextView = view.findViewById(R.id.caseCityText)
        val description: TextView = view.findViewById(R.id.caseDescriptionText)
        val detailsButton: Button = view.findViewById(R.id.viewCaseDetailsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpCaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_help_case, parent, false)
        return HelpCaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpCaseViewHolder, position: Int) {
        val case = cases[position]
        holder.title.text = case.title
        holder.category.text = case.category
        holder.city.text = case.city
        holder.description.text = case.description

        holder.detailsButton.setOnClickListener {
            val context = holder.itemView.context
            val amountLine = if (case.amountNeeded.isNotBlank()) "\n${case.amountNeeded}" else ""
            val message = "${case.description}$amountLine\n\n${case.charityName}\n${case.charityContact}"

            AlertDialog.Builder(context)
                .setTitle(case.title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun getItemCount(): Int = cases.size
    }
