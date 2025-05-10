package com.haseebali.savelife.models

data class Conversation(
    val id: String = "",
    val participants: Map<String, Boolean> = mapOf(),
    val lastMessage: Message? = null,
    val otherUserName: String = "",
    val otherUserProfilePicture: String = ""
) 