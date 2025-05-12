package com.haseebali.savelife.models

import com.haseebali.savelife.models.User

data class UserWithDetails(
    val user: User,
    val bloodType: String = "",
    val country: String = "",
    val city: String = ""
) 