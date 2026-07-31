package com.ahmed.blooddonation

data class Campaign(
    var id: String = "",
    val hospitalId: String = "",
    val hospitalName: String = "",
    val title: String = "",
    val dateTimeText: String = "",
    val city: String = "",
    val notes: String = "",
    val timestamp: Long = 0L,
    val registeredCount: Int = 0
)
