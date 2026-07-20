package com.ahmed.blooddonation

data class HelpCase(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val amountNeeded: String = "",
    val city: String = "",
    val charityName: String = "",
    val charityContact: String = "",
    val requesterName: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val resolved: Boolean = false,
    val verificationCount: Int = 0
)
