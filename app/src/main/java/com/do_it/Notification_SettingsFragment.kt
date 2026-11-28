package com.do_it

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.do_it.databinding.FragmentNotificationSettingsBinding

class Notification_SettingsFragment : Fragment() {
    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!
    private var refreshing = false
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (isAdded) {
            savePushSetting(granted)
            if (!granted) Toast.makeText(requireContext(),
                "알림을 받으려면 휴대폰 알림 설정에서 허용해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val preferences = preferences()
        val switches = mapOf("meeting" to binding.switchMeeting,
            "friend" to binding.switchFriend, "chat" to binding.switchChat)
        switches.forEach { (key, switch) ->
            switch.isChecked = preferences.getBoolean(key, true)
            switch.setOnCheckedChangeListener { _, checked ->
                preferences.edit().putBoolean(key, checked).apply()
            }
        }
        binding.switchPush.setOnCheckedChangeListener { _, checked ->
            if (!refreshing) changePush(checked)
        }
        binding.buttonSystemNotifications.setOnClickListener { openSystemSettings() }
    }

    override fun onResume() {
        super.onResume()
        refreshPush()
    }

    private fun preferences() = NotificationPreferences.forUser(requireContext())

    private fun refreshPush() {
        val b = _binding ?: return
        val allowed = NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        val enabled = allowed && preferences().getBoolean("push_enabled", false)
        refreshing = true
        b.switchPush.isChecked = enabled
        refreshing = false
        b.switchMeeting.isEnabled = enabled
        b.switchFriend.isEnabled = enabled
        b.switchChat.isEnabled = enabled
        b.tvPushStatus.text = when {
            !allowed -> "휴대폰에서 앱 알림이 차단되어 있어요. 아래 버튼으로 설정을 열 수 있어요."
            !enabled -> "푸시알림을 켜면 받을 알림 종류를 선택할 수 있어요."
            else -> "아래에서 받고 싶은 알림을 선택해주세요."
        }
    }

    private fun changePush(checked: Boolean) {
        if (!checked) {
            savePushSetting(false)
        } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            refreshPush()
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            refreshPush()
            AlertDialog.Builder(requireContext()).setMessage("휴대폰 설정에서 앱 알림을 허용해주세요.")
                .setNegativeButton("취소", null)
                .setPositiveButton("설정 열기") { _, _ -> openSystemSettings() }.show()
        } else {
            savePushSetting(true)
        }
    }

    private fun savePushSetting(enabled: Boolean) {
        preferences().edit().putBoolean("push_enabled", enabled).apply()
        NotificationPreferences.syncPushSetting(requireContext())
        refreshPush()
    }

    private fun openSystemSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
