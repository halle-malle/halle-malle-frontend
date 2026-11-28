package com.do_it

data class userData (
    val id:String = "",
    val email:String = "",
    val nickname:String= "",
    val intro:String = "",
    val profileImage: String = "",
    val trustScore:Int= 0,
    val createdAt: Long=0L
)
data class friendData(
    val userId: String = "",    // 상대방 uid
    val status: Int = 0,        // 0 받은 요청, 1 친구
    val createdAt: Long = 0L
)
data class memberData (
    val meetingId:String="",
    val userId:String="",
    val name:String="",
    val intro:String = "",
    val role:Int = 1,
    val status:Int = 0,
    val waitingNo: Int = 0,
    val joinedAt:Long = 0L
) {
    companion object {
        const val ROLE_HOST = 0       // 방장
        const val ROLE_MEMBER = 1     // 멤버
        const val STATUS_JOINED = 0   // 참여 중
        const val STATUS_WAITING = 1  // 대기 중
    }
}

data class GroupData(
    val id: String = "",
    val memberIds: List<String> = listOf(), // 참가한 멤버들
    val hostId: String = "",          // 방장 uid
    val title: String = "",
    val category: String = "",        // 운동, 맛집, 스터디...
    val description: String = "",     // 상세 설명
    val dateTime: Long = 0L,          // 모임 일시
    val place: String = "",
    val address: String = "",         // 주소 (검색용)
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: Int = 0,              // 0 모집중, 1 확정, 2 취소, 3 종료
    val memberCount: Int = 0,
    val maxMember: Int = 0,
    val notice: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        const val STATUS_RECRUITING = 0  // 모집중
        const val STATUS_CONFIRMED = 1   // 확정
        const val STATUS_CANCELED = 2    // 취소
        const val STATUS_FINISHED = 3    // 종료
        const val END_TIME = 24 * 60 * 60 * 1000L   // 시작 후 하루 지나면 지난 모임
    }
}

data class chatData (
    val groupId:String ="",
    val senderId:String="",
    val createdAt:Long=0L,
    val name: String = "",
    val content: String = ""
) {
    companion object {
        const val TYPE_MINE = 0 //내 메시지
        const val TYPE_OTHER = 1 //상대 메시지
    }
}

data class reportData(
    val userId: String = "",         // 신고당한 사람 uid
    val count: Int = 0,              // 신고 횟수
    val reportedAt: Long = 0L        // 마지막 신고 시각
)
