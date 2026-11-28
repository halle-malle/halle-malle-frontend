package com.do_it

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.do_it.MyApplication.Companion.auth
import com.do_it.databinding.NotificationHistoryItemBinding
import com.google.firebase.functions.FirebaseFunctions
import java.text.DateFormat
import java.util.Date

object NotificationHistory {
    fun bind(
        activity: AppCompatActivity,
        button: View,
        onOpen: (Intent) -> Unit = { route ->
            val main = Intent(activity, MainActivity::class.java)
                .putExtras(route)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            activity.startActivity(main)
        }
    ) {
        button.setOnClickListener { show(activity, button, onOpen) }
    }

    private fun show(
        activity: AppCompatActivity, button: View, onOpen: (Intent) -> Unit,
        cursor: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        button.isEnabled = false
        val functions = FirebaseFunctions.getInstance("asia-northeast3")
        functions.getHttpsCallable("listNotifications")
            .call(mapOf("cursor" to cursor))
            .addOnCompleteListener { task ->
                button.isEnabled = true
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
                    auth.currentUser?.uid != uid
                ) return@addOnCompleteListener
                if (!task.isSuccessful) {
                    Toast.makeText(activity, "알림 기록을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                val result = task.result.data as? Map<*, *>
                val items = (result?.get("items") as? List<*>)
                    .orEmpty().filterIsInstance<Map<*, *>>()
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("알림 기록")
                    .setPositiveButton("닫기", null)
                if (items.isEmpty()) {
                    dialog.setMessage("알림 기록이 없어요.")
                } else {
                    val format = DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.SHORT
                    )
                    val adapter = object : ArrayAdapter<Map<*, *>>(
                        dialog.context, R.layout.notification_history_item, items
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val row = if (convertView == null) {
                                NotificationHistoryItemBinding.inflate(LayoutInflater.from(dialog.context), parent, false)
                            } else {
                                NotificationHistoryItemBinding.bind(convertView)
                            }
                            val item = items[position]
                            val millis = (item["createdAt"] as? Number)?.toLong()
                            row.notificationTitle.text = (item["title"] as? String).orEmpty()
                            row.notificationBody.text = (item["body"] as? String).orEmpty()
                            row.notificationTime.text = millis?.let { format.format(Date(it)) }.orEmpty()
                            row.notificationTime.visibility = if (millis == null) View.GONE else View.VISIBLE
                            return row.root
                        }
                    }
                    dialog.setAdapter(adapter) { _, index ->
                        val item = items[index]
                        val intent = Intent()
                            .putExtra("type", item["type"] as? String)
                            .putExtra("groupId", item["groupId"] as? String)
                            .putExtra("recipientUid", uid)
                        onOpen(intent)
                    }
                }
                val next = result?.get("nextCursor") as? String
                if (next != null) {
                    dialog.setNeutralButton("이전 알림") { _, _ -> show(activity, button, onOpen, next) }
                }
                dialog.show()
            }
    }
}
