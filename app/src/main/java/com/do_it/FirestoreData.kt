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
    val hostId: String = "",          // 방장 uid
    val title: String = "",
    val category: String = "",        // 운동, 맛집, 스터디...
    val description: String = "",     // 상세 설명
    val dateTime: Long = 0L,          // 모임 일시
    val place: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: Int = 0,              // 0 모집중, 1 확정, 2 취소, 3 종료
    val memberCount: Int = 0,
    val maxMember: Int = 0,
    val notice: String = "",
    val confirmDate: Long = 0L,       // 모집 확정일 (0이면 없음)
    val createdAt: Long = 0L
) {
    companion object {
        const val STATUS_RECRUITING = 0  // 모집중
        const val STATUS_CONFIRMED = 1   // 확정
        const val STATUS_CANCELED = 2    // 취소
        const val STATUS_FINISHED = 3    // 종료
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

