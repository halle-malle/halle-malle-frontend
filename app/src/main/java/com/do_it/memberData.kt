package com.do_it

data class memberData (
    val name:String,
    val intro:String = "",
    val isHost:String,
) {
    companion object {
        const val STATUS_LEADER = 0   // 방장
        const val STATUS_ISNOTFRIEND = 1  // 친구추가
    }
}