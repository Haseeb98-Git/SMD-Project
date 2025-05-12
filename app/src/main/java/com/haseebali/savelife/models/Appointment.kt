package com.haseebali.savelife.models

data class Appointment(
    var id: String = "",
    val donorId: String = "",
    val requesterId: String = "",
    val createdBy: String = "", // requester who initiated the appointment
    val country: String = "",
    val city: String = "",
    val venue: String = "",
    val date: String = "", // Format: YYYY-MM-DD
    val status: String = "pending", // pending, accepted, rejected, completed
    val completedDate: String = "", // Format: ISO 8601
    val createdAt: String = "" // Format: ISO 8601
) 