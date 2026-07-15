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

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.requestsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        val addButton = findViewById<Button>(R.id.addButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadRequests()

        addButton.setOnClickListener {
            startActivity(Intent(this, CreateRequestActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private fun loadRequests() {
        db.collection("requests")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val requests = mutableListOf<Request>()
                for (doc in result) {
                    val request = doc.toObject(Request::class.java)
                    request.id = doc.id
                    requests.add(request)
                }

                if (requests.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = RequestAdapter(requests)
                }
            }
    }
}
