package com.do_it

data class cardData(
    val id: Int,
    val title: String,
    val date: String,          // "10.25 (일) 19:00"
    val place: String,
    val status: Int,           // STATUS_JOINED / STATUS_WAITING / STATUS_HOST
    val isConfirmed: Boolean = false,  // 모임 확정 여부
    val waitingNo: Int = 0,    // 대기 순번
    val memberCount: Int = 0,
    val maxMember: Int = 0,
    val notice: String = "",
    val dDay: String = "",     // "내일 17:00 · D-1"
    val newMessage: Int = 0
) {
    companion object {
        const val STATUS_JOINED = 0   // 참여 중
        const val STATUS_WAITING = 1  // 대기 중
        const val STATUS_HOST = 2     // 내가 연 모임
        const val STATUS_UPCOMING = 3 // 그냥 유령 코드(쓰이지 않음)
    }
}