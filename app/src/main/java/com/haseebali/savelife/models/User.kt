package com.haseebali.savelife.models

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val profilePicture: String = "",
    val roles: Roles = Roles(),
    val donorAvailability: String = "",
    val createdAt: String = ""
) {
    companion object {}
} 