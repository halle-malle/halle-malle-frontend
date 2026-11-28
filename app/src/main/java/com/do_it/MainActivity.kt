package com.do_it

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.do_it.databinding.ActivityMainBinding
import androidx.navigation.NavController
import androidx.navigation.ui.navigateUp
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import android.content.pm.PackageManager
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import android.Manifest
import android.app.NotificationManager
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar4)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        requestNotificationPermission()
        refreshFcmToken()
        val navView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
                as NavHostFragment

        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.quest,
                R.id.my
            )
        )

        setupActionBarWithNavController(
            navController,
            appBarConfiguration
        )
        navController.addOnDestinationChangedListener { _, _, _ ->
            supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_24)
        }
        navView.setupWithNavController(navController)

        binding.btnNotification.setOnClickListener {
            Toast.makeText(this, "알림 클릭", Toast.LENGTH_SHORT).show()
        }

        binding.btnSettings.setOnClickListener {
            val modal = ModalBottomSheetFragment()
            modal.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogBorder20WhiteTheme)
            
            modal.show(supportFragmentManager, "settings")

        }

        if (savedInstanceState == null) openNotification(intent)
    }

    private fun openNotification(source: Intent) {
        val uid = auth.currentUser?.uid ?: return
        if (source.getStringExtra("recipientUid") != uid) return

        val type = source.getStringExtra("type")
        if (type == "friend_request") {
            navController.navigate(R.id.friendFragment)
            return
        }

        val groupId = source.getStringExtra("groupId") ?: return
        if (groupId.isBlank() || '/' in groupId) return
        val destination = when (type) {
            "group_chat" -> GroupChatActivity::class.java
            "group_application", "group_membership", "group_approval", "group_start" -> GroupDetailActivity::class.java
            else -> return
        }

        db.collection("groups").document(groupId).get()
            .addOnSuccessListener(this) { document ->
                val group = document.toObject(GroupData::class.java)
                val canOpen = group != null && if (type == "group_application" || type == "group_membership") {
                    group.hostId == uid
                } else {
                    group.hostId == uid || group.memberIds.contains(uid)
                }
                if (auth.currentUser?.uid != uid) return@addOnSuccessListener
                if (canOpen) {
                    val groupIntent = Intent(this, destination)
                    groupIntent.putExtra("groupId", groupId)
                    startActivity(groupIntent)
                    if (type == "group_chat") {
                        val manager = getSystemService(NotificationManager::class.java)
                        val tag = "group_chat:$uid:$groupId"
                        for (notification in manager.activeNotifications) {
                            if (notification.tag == tag) {
                                manager.cancel(tag, notification.id)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "모임을 열 수 없어요.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener(this) {
                Toast.makeText(this, "모임 정보를 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    private fun refreshFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                MyApplication.saveFcmToken(uid, token)
            }
            .addOnFailureListener {
                Log.e("FCM", "토큰 조회 실패", it)
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }
}
