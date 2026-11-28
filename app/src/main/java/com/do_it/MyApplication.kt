package com.do_it
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication: Application() {
    companion object {
        lateinit var auth: FirebaseAuth // 사용자 인증객체
        lateinit var db: FirebaseFirestore // firestore 데이터 베이스 객체


        fun saveFcmToken(uid: String, token: String) {
            if (auth.currentUser?.uid != uid || token.isBlank()) return

            val data = hashMapOf(
                "fcmToken" to token,
                "tokenUpdatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(data)
                .addOnFailureListener {
                    Log.e("FCM", "토큰 저장 실패", it)
                }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            "할래말래 알림",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        // FirebaseAuth 객체 생성
        auth= FirebaseAuth.getInstance()
        auth.useAppLanguage() // 앱의 언어환경정보에 기반하여 언어 설정
        // FirebaseFirestore 객체 생성
        db= FirebaseFirestore.getInstance()
    }
}

