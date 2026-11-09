package com.do_it

data class chatData (
    val name: String = "",
    val content: String,
    val time: String,
    val type: Int
) {
    companion object {
        const val TYPE_MINE = 0 //내 메시지
        const val TYPE_OTHER = 1 //상대 메시지
    }
}