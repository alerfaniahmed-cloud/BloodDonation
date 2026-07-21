package com.ahmed.blooddonation

data class Request(
    var id: String = "",
    var bloodType: String = "",
    var city: String = "",
    var urgency: String = "",
    var contactPhone: String = "",
    var notes: String = "",
    var requesterName: String = "",
    var timestamp: Long = 0L,
    var userId: String = "",
    var requesterType: String = "individual",
    var lat: Double? = null,
    var lng: Double? = null,
    @JvmField @Transient var distanceKm: Double? = null
)
