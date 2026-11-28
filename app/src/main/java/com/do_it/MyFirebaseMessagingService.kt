package com.do_it

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private val nextNotificationId = java.util.concurrent.atomic.AtomicInteger(
            System.currentTimeMillis().toInt()
        )
    }

    // 메시지 수신
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data["type"] == "group_chat" &&
            GroupChatActivity.isViewing(data["groupId"], data["recipientUid"])
        ) {
            return
        }

        val title = message.notification?.title ?: "할래말래?"
        val body = message.notification?.body ?: ""

        showNotification(title, body, data)
    }

    // 토큰 갱신
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = MyApplication.auth.currentUser?.uid ?: return
        MyApplication.saveFcmToken(uid, token)
    }

    // 알림 표시
    private fun showNotification(title: String, body: String, data: Map<String, String>) {

        // 알림 권한 확인
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // 알림을 클릭하면 앱으로 이동
        val notificationId = nextNotificationId.incrementAndGet()
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("type", data["type"])
        intent.putExtra("groupId", data["groupId"])
        intent.putExtra("recipientUid", data["recipientUid"])

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 관리자
        val manager = getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager

        // 알림 생성
        val notification = NotificationCompat.Builder(
            this, getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // 알림 출력
        val groupId = data["groupId"]
        val recipientUid = data["recipientUid"]
        val tag = if (data["type"] == "group_chat" && groupId != null &&
            recipientUid != null
        ) {
            "group_chat:$recipientUid:$groupId"
        } else {
            null
        }
        manager.notify(tag, notificationId, notification)
    }
}