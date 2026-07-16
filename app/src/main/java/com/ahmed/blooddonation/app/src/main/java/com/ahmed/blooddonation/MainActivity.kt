package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var bloodTypeFilterSpinner: Spinner
    private lateinit var cityFilterInput: EditText

    private var allRequests: List<Request> = listOf()

    private val bloodTypes = arrayOf("الكل", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.requestsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        bloodTypeFilterSpinner = findViewById(R.id.bloodTypeFilterSpinner)
        cityFilterInput = findViewById(R.id.cityFilterInput)
        val addButton = findViewById<Button>(R.id.addButton)
        val profileButton = findViewById<Button>(R.id.profileButton)
        val hospitalsButton = findViewById<Button>(R.id.hospitalsButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bloodTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bloodTypeFilterSpinner.adapter = spinnerAdapter

        bloodTypeFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        cityFilterInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        loadRequests()

        addButton.setOnClickListener {
            startActivity(Intent(this, CreateRequestActivity::class.java))
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        hospitalsButton.setOnClickListener {
            startActivity(Intent(this, HospitalsActivity::class.java))
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
                allRequests = requests
                applyFilters()
            }
    }

    private fun applyFilters() {
        val selectedBloodType = bloodTypeFilterSpinner.selectedItem?.toString() ?: "الكل"
        val cityQuery = cityFilterInput.text.toString().trim()

        val filtered = allRequests.filter { request ->
            val matchesBloodType = selectedBloodType == "الكل" || request.bloodType == selectedBloodType
            val matchesCity = cityQuery.isEmpty() || request.city.contains(cityQuery, ignoreCase = true)
            matchesBloodType && matchesCity
        }

        if (filtered.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = RequestAdapter(filtered)
        }
    }
}
