package com.ahmed.blooddonation

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.FirebaseFirestore

class AdminRequestsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_requests)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.adminRequestsRecyclerView)
        emptyText = findViewById(R.id.adminEmptyText)
        swipeRefreshLayout = findViewById(R.id.adminSwipeRefreshLayout)
        val backButton = findViewById<Button>(R.id.adminBackButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            finish()
        }

        swipeRefreshLayout.setOnRefreshListener {
            loadAllRequests()
        }

        loadAllRequests()
    }

    private fun loadAllRequests() {
        swipeRefreshLayout.isRefreshing = true
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
                    recyclerView.adapter = AdminRequestAdapter(requests) { request ->
                        confirmAndDelete(request)
                    }
                }

                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun confirmAndDelete(request: Request) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message, request.bloodType, request.city))
            .setPositiveButton(R.string.delete_request_button) { _, _ ->
                deleteRequest(request)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun deleteRequest(request: Request) {
        db.collection("requests").document(request.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.request_deleted), Toast.LENGTH_SHORT).show()
                loadAllRequests()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
            }
    }
}
