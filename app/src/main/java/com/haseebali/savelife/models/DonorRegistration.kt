package com.haseebali.savelife.models

data class DonorRegistration(
    val bloodType: String = "",
    val country: String = "",
    val city: String = "",
    val address: String = "",
    val healthStatus: String = "", // "Healthy" | "Deferred"
    val description: String = "",
    val updatedAt: Any = "" // Can be either Long or String
) {
    companion object {}
} 