package com.ahmed.blooddonation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class DonorLeaderboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var cityTitleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_leaderboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.leaderboardRecyclerView)
        emptyText = findViewById(R.id.emptyLeaderboardText)
        cityTitleText = findViewById(R.id.leaderboardCityText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadUserCityThenLeaderboard()
    }

    private fun loadUserCityThenLeaderboard() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val city = doc.getString("city") ?: ""
                if (city.isBlank()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return@addOnSuccessListener
                }
                cityTitleText.text = getString(R.string.leaderboard_city_prefix, city)
                loadLeaderboard(city)
            }
    }

    private fun loadLeaderboard(city: String) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis

        db.collection("donorOffers")
            .whereEqualTo("city", city)
            .get()
            .addOnSuccessListener { result ->
                val counts = mutableMapOf<String, Int>()
                val names = mutableMapOf<String, String>()

                for (doc in result) {
                    val donorConfirmed = doc.getBoolean("donorConfirmed") ?: false
                    val hospitalConfirmed = doc.getBoolean("hospitalConfirmed") ?: false
                    if (!donorConfirmed && !hospitalConfirmed) continue

                    val completedTimestamp = doc.getLong("completedTimestamp") ?: 0L
                    if (completedTimestamp < monthStart) continue

                    val donorId = doc.getString("donorId") ?: continue
                    val donorName = doc.getString("donorName") ?: ""

                    counts[donorId] = (counts[donorId] ?: 0) + 1
                    names[donorId] = donorName
                }

                val entries = counts.entries
                    .map { LeaderboardEntry(names[it.key] ?: "", it.value) }
                    .sortedByDescending { it.donationCount }
                    .take(10)

                if (entries.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = DonorLeaderboardAdapter(entries)
                }
            }
    }
}
