package com.do_it

import android.content.Context

object NotificationPreferences {
    fun forUser(context: Context) = context.getSharedPreferences(
        "notifications_${MyApplication.auth.currentUser?.uid ?: "guest"}", Context.MODE_PRIVATE)
}
