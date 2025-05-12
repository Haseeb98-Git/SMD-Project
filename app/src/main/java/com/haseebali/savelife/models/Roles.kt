package com.haseebali.savelife.models

data class Roles(
    val donor: Boolean = false,
    val requester: Boolean = false
) {
    init {
        require(!(donor && requester)) { "User cannot be both donor and requester" }
    }
} 