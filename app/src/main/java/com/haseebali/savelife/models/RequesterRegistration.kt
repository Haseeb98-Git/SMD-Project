package com.haseebali.savelife.models

data class RequesterRegistration(
    val bloodType: String = "",
    val country: String = "",
    val city: String = "",
    val address: String = "",
    val urgency: String = "", // "immediate" | "within 24 hours" | "within 3 days" | "other"
    val description: String = "",
    val updatedAt: Any = "" // Can be either Long or String
) {
    companion object {}
} 