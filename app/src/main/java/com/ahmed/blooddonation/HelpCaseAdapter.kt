package com.ahmed.blooddonation

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HelpCaseAdapter(private val cases: MutableList<HelpCase>) :
    RecyclerView.Adapter<HelpCaseAdapter.HelpCaseViewHolder>() {

    class HelpCaseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.caseTitleText)
        val category: TextView = view.findViewById(R.id.caseCategoryText)
        val city: TextView = view.findViewById(R.id.caseCityText)
        val description: TextView = view.findViewById(R.id.caseDescriptionText)
        val detailsButton: Button = view.findViewById(R.id.viewCaseDetailsButton)
        val ownerActionsRow: LinearLayout = view.findViewById(R.id.ownerActionsRow)
        val editButton: Button = view.findViewById(R.id.editCaseButton)
        val deleteButton: Button = view.findViewById(R.id.deleteCaseButton)
        val resolveButton: Button = view.findViewById(R.id.resolveCaseButton)
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

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = currentUserId != null && currentUserId == case.userId
        holder.ownerActionsRow.visibility = if (isOwner) View.VISIBLE else View.GONE

        holder.editButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CreateHelpCaseActivity::class.java).apply {
                putExtra("caseId", case.id)
                putExtra("title", case.title)
                putExtra("description", case.description)
                putExtra("category", case.category)
                putExtra("amountNeeded", case.amountNeeded)
                putExtra("city", case.city)
                putExtra("charityName", case.charityName)
                putExtra("charityContact", case.charityContact)
                putExtra("timestamp", case.timestamp)
            }
            context.startActivity(intent)
        }

        holder.deleteButton.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.confirm_delete_case_title))
                .setMessage(context.getString(R.string.confirm_delete_case_message))
                .setPositiveButton(context.getString(R.string.confirm_button)) { dialog, _ ->
                    FirebaseFirestore.getInstance().collection("helpCases")
                        .document(case.id)
                        .delete()
                        .addOnSuccessListener {
                            val currentPosition = holder.adapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                cases.removeAt(currentPosition)
                                notifyItemRemoved(currentPosition)
                                Toast.makeText(context, context.getString(R.string.help_case_deleted), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, context.getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton(context.getString(R.string.cancel_button)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        holder.resolveButton.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.confirm_resolve_case_title))
                .setMessage(context.getString(R.string.confirm_resolve_case_message))
                .setPositiveButton(context.getString(R.string.confirm_button)) { dialog, _ ->
                    FirebaseFirestore.getInstance().collection("helpCases")
                        .document(case.id)
                        .update("resolved", true)
                        .addOnSuccessListener {
                            val currentPosition = holder.adapterPosition
                            if (currentPosition != RecyclerView.NO_POSITION) {
                                cases.removeAt(currentPosition)
                                notifyItemRemoved(currentPosition)
                                Toast.makeText(context, context.getString(R.string.help_case_resolved), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, context.getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton(context.getString(R.string.cancel_button)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun getItemCount(): Int = cases.size
}
