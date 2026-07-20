package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HelpCasesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_cases)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.helpCasesRecyclerView)
        emptyText = findViewById(R.id.emptyHelpCasesText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val createButton = findViewById<Button>(R.id.createHelpCaseButton)
        createButton.setOnClickListener {
            startActivity(Intent(this, CreateHelpCaseActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadHelpCases()
    }

    private fun loadHelpCases() {
        db.collection("helpCases")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val cases = mutableListOf<HelpCase>()
                for (doc in result) {
                    val case = doc.toObject(HelpCase::class.java)
                    case.id = doc.id
                    if (!case.resolved) {
                        cases.add(case)
                    }
                }

                if (cases.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = HelpCaseAdapter(cases)
                }
            }
    }
}
