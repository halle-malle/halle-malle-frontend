package com.do_it

import android.content.Context
import android.util.Log

object NotificationPreferences {
    fun forUser(context: Context) = context.getSharedPreferences(
        "notifications_${MyApplication.auth.currentUser?.uid ?: "guest"}", Context.MODE_PRIVATE)

    fun isPushEnabled(context: Context) = forUser(context).getBoolean("push_enabled", false)

    fun syncPushSetting(context: Context) {
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.db.collection("users").document(uid)
            .update("pushEnabled", isPushEnabled(context))
            .addOnFailureListener { Log.e("FCM", "푸시 알림 설정 저장 실패", it) }
    }
}
