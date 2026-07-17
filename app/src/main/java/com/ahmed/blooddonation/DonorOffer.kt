package com.ahmed.blooddonation

data class DonorOffer(
    var id: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val donorPhone: String = "",
    val bloodType: String = "",
    val city: String = "",
    val targetHospitalId: String = "",
    val targetHospitalName: String = "",
    val timestamp: Long = 0L
)
