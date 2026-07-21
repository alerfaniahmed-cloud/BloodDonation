package com.ahmed.blooddonation

data class FamilyCircle(
    var id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val creatorId: String = "",
    val timestamp: Long = 0L
)

data class CircleMemberInfo(
    val userId: String = "",
    val name: String = "",
    val bloodType: String = "",
    val phone: String = "",
    val city: String = ""
)
