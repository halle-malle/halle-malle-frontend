package com.do_it

import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.do_it.MyApplication.Companion.auth
import com.do_it.databinding.ActivityNotificationHistoryBinding
import com.do_it.databinding.NotificationHistoryItemBinding
import com.google.firebase.functions.FirebaseFunctions
import java.text.DateFormat
import java.util.Date

class NotificationHistoryActivity : AppCompatActivity() {
    companion object {
        fun bind(activity: AppCompatActivity, button: View) {
            button.setOnClickListener {
                activity.startActivity(Intent(activity, NotificationHistoryActivity::class.java))
            }
            attachUnreadBadge(activity, button)
        }

        @androidx.annotation.OptIn(ExperimentalBadgeUtils::class)
        private fun attachUnreadBadge(activity: AppCompatActivity, button: View) {
            val badge = BadgeDrawable.create(activity)
            val inset = (8 * activity.resources.displayMetrics.density).toInt()
            badge.horizontalOffset = inset
            badge.verticalOffset = inset
            badge.isVisible = false
            button.post {
                if (!activity.isDestroyed) BadgeUtils.attachBadgeDrawable(badge, button)
            }
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                private var requestVersion = 0

                override fun onStart(owner: LifecycleOwner) {
                    val uid = auth.currentUser?.uid ?: return
                    MyApplication.db.collection("notificationInboxes").document(uid)
                        .addSnapshotListener(activity) { snapshot, error ->
                            if (snapshot == null || error != null) return@addSnapshotListener
                            val version = ++requestVersion
                            loadUnreadCount { count ->
                                if (version != requestVersion || count == null ||
                                    auth.currentUser?.uid != uid ||
                                    !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                                ) return@loadUnreadCount
                                badge.number = count
                                badge.isVisible = count > 0
                                button.contentDescription =
                                    if (count == 0) "알림" else "알림, 읽지 않은 ${count}개"
                            }
                        }
                }

                override fun onStop(owner: LifecycleOwner) {
                    requestVersion++
                }
            })
        }

        private fun loadUnreadCount(onResult: (Int?) -> Unit) {
            FirebaseFunctions.getInstance("asia-northeast3")
                .getHttpsCallable("getNotificationUnreadCount")
                .call()
                .addOnCompleteListener { task ->
                    val data = if (task.isSuccessful) task.result.data as? Map<*, *> else null
                    onResult((data?.get("unread") as? Number)?.toInt())
                }
        }

        fun markAsRead(id: String, onResult: (Boolean) -> Unit = {}) {
            FirebaseFunctions.getInstance("asia-northeast3")
                .getHttpsCallable("markNotificationRead")
                .call(mapOf("id" to id))
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) Log.w("NotificationHistory", "읽음 처리 실패", task.exception)
                    onResult(task.isSuccessful)
                }
        }
    }

    private lateinit var binding: ActivityNotificationHistoryBinding
    private lateinit var adapter: ArrayAdapter<Map<*, *>>
    private var nextCursor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        binding = ActivityNotificationHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        binding.clearButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("알림 모두 삭제")
                .setMessage("지금까지 받은 알림 기록을 모두 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제") { _, _ -> clearHistory(uid) }
                .show()
        }
        binding.moreButton.setOnClickListener { nextCursor?.let { loadHistory(uid, it) } }
        binding.retryButton.setOnClickListener { loadHistory(uid) }

        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        adapter = object : ArrayAdapter<Map<*, *>>(this, R.layout.notification_history_item) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = if (convertView == null) {
                    NotificationHistoryItemBinding.inflate(layoutInflater, parent, false)
                } else {
                    NotificationHistoryItemBinding.bind(convertView)
                }
                val item = getItem(position)
                val millis = (item?.get("createdAt") as? Number)?.toLong()
                row.notificationTitle.text = (item?.get("title") as? String).orEmpty()
                val read = item?.get("read") == true
                row.notificationTitle.setTypeface(null, if (read) Typeface.NORMAL else Typeface.BOLD)
                val body = (item?.get("body") as? String).orEmpty()
                row.notificationBody.text = body
                row.notificationBody.visibility = if (body.isEmpty()) View.GONE else View.VISIBLE
                row.notificationTime.text = millis?.let { format.format(Date(it)) }.orEmpty()
                row.notificationTime.visibility = if (millis == null) View.GONE else View.VISIBLE
                row.notificationIcon.setImageResource(when (item?.get("type")) {
                    "group_chat" -> android.R.drawable.ic_dialog_email
                    "group_cancellation" -> android.R.drawable.ic_delete
                    else -> R.drawable.ic_notifications
                })
                return row.root
            }
        }
        binding.historyList.adapter = adapter
        binding.historyList.setOnItemClickListener { _, _, position, _ ->
            if (auth.currentUser?.uid != uid) {
                finish()
                return@setOnItemClickListener
            }
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            val route = Intent(this, MainActivity::class.java)
                .putExtra("type", item["type"] as? String)
                .putExtra("groupId", item["groupId"] as? String)
                .putExtra("recipientUid", uid)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val id = item["id"] as? String
            if (id == null || item["read"] == true) {
                startActivity(route)
                finish()
                return@setOnItemClickListener
            }
            binding.historyList.isEnabled = false
            markAsRead(id) { success ->
                if (isFinishing || isDestroyed) return@markAsRead
                binding.historyList.isEnabled = true
                if (auth.currentUser?.uid != uid) {
                    finish()
                } else if (success) {
                    startActivity(route)
                    finish()
                } else {
                    Toast.makeText(this, "읽음 상태를 저장하지 못했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        loadHistory(uid)
    }

    private fun clearHistory(uid: String) {
        if (auth.currentUser?.uid != uid) return
        binding.clearButton.isEnabled = false
        binding.moreButton.isEnabled = false
        binding.loading.visibility = View.VISIBLE
        FirebaseFunctions.getInstance("asia-northeast3")
            .getHttpsCallable("clearNotificationHistory")
            .call()
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (auth.currentUser?.uid != uid) {
                    finish()
                    return@addOnCompleteListener
                }
                val message = if (task.isSuccessful) {
                    "알림 기록을 삭제했어요."
                } else {
                    "삭제를 완료하지 못했어요. 남은 기록을 다시 불러올게요."
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                adapter.clear()
                nextCursor = null
                binding.moreButton.visibility = View.GONE
                loadHistory(uid)
            }
    }

    private fun loadHistory(uid: String, cursor: String? = null) {
        binding.clearButton.isEnabled = false
        binding.loading.visibility = View.VISIBLE
        binding.retryButton.visibility = View.GONE
        binding.moreButton.isEnabled = false
        FirebaseFunctions.getInstance("asia-northeast3")
            .getHttpsCallable("listNotifications")
            .call(mapOf("cursor" to cursor))
            .addOnCompleteListener { task ->
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (auth.currentUser?.uid != uid) {
                    finish()
                    return@addOnCompleteListener
                }
                binding.loading.visibility = View.GONE
                binding.moreButton.isEnabled = true
                binding.clearButton.isEnabled = !adapter.isEmpty
                if (!task.isSuccessful) {
                    if (adapter.isEmpty) {
                        binding.emptyMessage.text = "알림을 불러오지 못했어요."
                        binding.emptyMessage.visibility = View.VISIBLE
                        binding.retryButton.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, "이전 알림을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                    }
                    return@addOnCompleteListener
                }

                val result = task.result.data as? Map<*, *>
                val items = (result?.get("items") as? List<*>)
                    .orEmpty().filterIsInstance<Map<*, *>>()
                adapter.addAll(items)
                binding.clearButton.isEnabled = !adapter.isEmpty
                nextCursor = result?.get("nextCursor") as? String
                binding.moreButton.visibility = if (nextCursor == null) View.GONE else View.VISIBLE
                binding.emptyMessage.text = "아직 받은 알림이 없어요."
                binding.emptyMessage.visibility = if (adapter.isEmpty) View.VISIBLE else View.GONE
                loadUnreadCount { count ->
                    if (!isFinishing && !isDestroyed && auth.currentUser?.uid == uid) {
                        binding.allReadMessage.visibility =
                            if (!adapter.isEmpty && count == 0) View.VISIBLE else View.GONE
                    }
                }
            }
    }
}
