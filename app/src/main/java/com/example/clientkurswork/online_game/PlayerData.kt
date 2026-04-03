package com.example.clientkurswork.online_game

enum class VoteMessage {
    NOTHING,
    ACCEPTED,
    DECLINED
}

data class PlayerData(
    var username: String,
    var voteResult: VoteMessage = VoteMessage.NOTHING
)
