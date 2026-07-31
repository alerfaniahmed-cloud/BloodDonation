package com.ahmed.blooddonation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CircleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var joinCreateLayout: View
    private lateinit var circleInfoLayout: View
    private lateinit var circleNameInput: EditText
    private lateinit var createCircleButton: Button
    private lateinit var joinCodeInput: EditText
    private lateinit var joinCircleButton: Button
    private lateinit var circleNameText: TextView
    private lateinit var inviteCodeText: TextView
    private lateinit var shareCodeButton: Button
    private lateinit var emergencyCircleButton: Button
    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var emptyMembersText: TextView
    private lateinit var leaveCircleButton: Button
    private lateinit var familyTreeCard: CardView
    private lateinit var familyDonationCountText: TextView
    private lateinit var familyLivesSavedText: TextView
    private lateinit var familyContributorsText: TextView

    private var myCircleId: String = ""
    private var myBloodType: String = ""
    private var myCity: String = ""
    private var myName: String = ""
    private var myPhone: String = ""

    companion object {
        private const val LIVES_PER_DONATION = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        joinCreateLayout = findViewById(R.id.joinCreateLayout)
        circleInfoLayout = findViewById(R.id.circleInfoLayout)
        circleNameInput = findViewById(R.id.circleNameInput)
        createCircleButton = findViewById(R.id.createCircleButton)
        joinCodeInput = findViewById(R.id.joinCodeInput)
        joinCircleButton = findViewById(R.id.joinCircleButton)
        circleNameText = findViewById(R.id.circleNameText)
        inviteCodeText = findViewById(R.id.inviteCodeText)
        shareCodeButton = findViewById(R.id.shareCodeButton)
        emergencyCircleButton = findViewById(R.id.emergencyCircleButton)
        membersRecyclerView = findViewById(R.id.membersRecyclerView)
        emptyMembersText = findViewById(R.id.emptyMembersText)
        leaveCircleButton = findViewById(R.id.leaveCircleButton)
        familyTreeCard = findViewById(R.id.familyTreeCard)
        familyDonationCountText = findViewById(R.id.familyDonationCountText)
        familyLivesSavedText = findViewById(R.id.familyLivesSavedText)
        familyContributorsText = findViewById(R.id.familyContributorsText)

        membersRecyclerView.layoutManager = LinearLayoutManager(this)

        createCircleButton.setOnClickListener { createCircle() }
        joinCircleButton.setOnClickListener { joinCircle() }
        shareCodeButton.setOnClickListener { shareInviteCode() }
        emergencyCircleButton.setOnClickListener { sendCircleAlert() }
        leaveCircleButton.setOnClickListener { confirmLeaveCircle() }
    }

    override fun onResume() {
        super.onResume()
        loadMyProfile()
    }

    private fun loadMyProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                myCircleId = doc.getString("circleId") ?: ""
                myBloodType = doc.getString("bloodType") ?: ""
                myCity = doc.getString("city") ?: ""
                myName = doc.getString("name") ?: getString(R.string.default_requester_name)
                myPhone = doc.getString("phone") ?: ""

                if (myCircleId.isBlank()) {
                    joinCreateLayout.visibility = View.VISIBLE
                    circleInfoLayout.visibility = View.GONE
                } else {
                    joinCreateLayout.visibility = View.GONE
                    circleInfoLayout.visibility = View.VISIBLE
                    loadCircleInfo()
                }
            }
    }

    private fun createCircle() {
        val name = circleNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.circle_name_required), Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid ?: return
        val code = generateInviteCode()

        val circle = hashMapOf(
            "name" to name,
            "inviteCode" to code,
            "creatorId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("circles").add(circle)
            .addOnSuccessListener { docRef ->
                db.collection("users").document(userId)
                    .update("circleId", docRef.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.circle_created), Toast.LENGTH_SHORT).show()
                        loadMyProfile()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun joinCircle() {
        val code = joinCodeInput.text.toString().trim().uppercase()
        if (code.isEmpty()) {
            Toast.makeText(this, getString(R.string.circle_code_required), Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid ?: return

        db.collection("circles").whereEqualTo("inviteCode", code).get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(this, getString(R.string.circle_code_invalid), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val circleDoc = result.documents[0]
                db.collection("users").document(userId)
                    .update("circleId", circleDoc.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.circle_joined), Toast.LENGTH_SHORT).show()
                        loadMyProfile()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
            }
    }

    private fun loadCircleInfo() {
        db.collection("circles").document(myCircleId).get()
            .addOnSuccessListener { doc ->
                val circleName = doc.getString("name") ?: ""
                val inviteCode = doc.getString("inviteCode") ?: ""
                circleNameText.text = circleName
                inviteCodeText.text = getString(R.string.circle_invite_code_prefix, inviteCode)
            }
        loadMembers()
    }

    private fun loadMembers() {
        db.collection("users").whereEqualTo("circleId", myCircleId).get()
            .addOnSuccessListener { result ->
                val members = mutableListOf<CircleMemberInfo>()
                for (doc in result) {
                    members.add(
                        CircleMemberInfo(
                            userId = doc.id,
                            name = doc.getString("name") ?: "",
                            bloodType = doc.getString("bloodType") ?: "",
                            phone = doc.getString("phone") ?: "",
                            city = doc.getString("city") ?: ""
                        )
                    )
                }
                if (members.isEmpty()) {
                    emptyMembersText.visibility = View.VISIBLE
                    membersRecyclerView.visibility = View.GONE
                } else {
                    emptyMembersText.visibility = View.GONE
                    membersRecyclerView.visibility = View.VISIBLE
                    membersRecyclerView.adapter = CircleMemberAdapter(members)
                }
                loadFamilyTree(members)
            }
    }

    private fun loadFamilyTree(members: List<CircleMemberInfo>) {
        if (members.isEmpty()) {
            familyTreeCard.visibility = View.GONE
            return
        }

        var totalDonations = 0
        var contributorsCount = 0
        var queriesCompleted = 0

        for (member in members) {
            db.collection("donorOffers")
                .whereEqualTo("donorId", member.userId)
                .get()
                .addOnSuccessListener { result ->
                    var memberDonations = 0
                    for (doc in result) {
                        val donorConfirmed = doc.getBoolean("donorConfirmed") ?: false
                        val hospitalConfirmed = doc.getBoolean("hospitalConfirmed") ?: false
                        if (donorConfirmed || hospitalConfirmed) {
                            memberDonations++
                        }
                    }
                    if (memberDonations > 0) {
                        contributorsCount++
                    }
                    totalDonations += memberDonations
                    queriesCompleted++

                    if (queriesCompleted == members.size) {
                        displayFamilyTree(totalDonations, contributorsCount, members.size)
                    }
                }
                .addOnFailureListener {
                    queriesCompleted++
                    if (queriesCompleted == members.size) {
                        displayFamilyTree(totalDonations, contributorsCount, members.size)
                    }
                }
        }
    }

    private fun displayFamilyTree(totalDonations: Int, contributorsCount: Int, totalMembers: Int) {
        if (totalDonations <= 0) {
            familyTreeCard.visibility = View.GONE
            return
        }

        familyTreeCard.visibility = View.VISIBLE
        familyDonationCountText.text = totalDonations.toString()
        val livesSaved = totalDonations * LIVES_PER_DONATION
        familyLivesSavedText.text = getString(R.string.family_lives_saved_text, livesSaved)
        familyContributorsText.text = getString(R.string.family_contributors_text, contributorsCount, totalMembers)
    }

    private fun shareInviteCode() {
        val code = inviteCodeText.text.toString()
        val shareText = getString(R.string.circle_share_text, circleNameText.text.toString(), code)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.circle_invite_share_title)))
    }

    private fun sendCircleAlert() {
        val userId = auth.currentUser?.uid ?: return

        if (myBloodType.isBlank() || myPhone.isBlank()) {
            Toast.makeText(this, getString(R.string.complete_profile_first), Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_circle_alert_title))
            .setMessage(getString(R.string.confirm_circle_alert_message))
            .setPositiveButton(getString(R.string.confirm_button)) { _, _ ->
                val alert = hashMapOf(
                    "circleId" to myCircleId,
                    "senderId" to userId,
                    "senderName" to myName,
                    "senderPhone" to myPhone,
                    "bloodType" to myBloodType,
                    "city" to myCity,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("circleAlerts").add(alert)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.circle_alert_sent), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.error_generic, e.message), Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun confirmLeaveCircle() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_leave_circle_title))
            .setMessage(getString(R.string.confirm_leave_circle_message))
            .setPositiveButton(getString(R.string.confirm_button)) { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .update("circleId", "")
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.circle_left), Toast.LENGTH_SHORT).show()
                        loadMyProfile()
                    }
            }
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show()
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
