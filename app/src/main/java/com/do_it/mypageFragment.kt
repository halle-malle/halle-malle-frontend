package com.do_it

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FragmentMypageBinding
import com.google.firebase.firestore.ListenerRegistration

class mypageFragment : Fragment() {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private val listeners = mutableListOf<ListenerRegistration>()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.menuFriends.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_friendFragment)
        }
        binding.menuPastMeetups.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_previous_meetingFragment)
        }
        binding.menuNotificationSettings.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_notification_SettingsFragment)
        }
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }
        binding.menuTerms.setOnClickListener { openInfo("terms") }
        binding.menuPrivacy.setOnClickListener { openInfo("privacy") }
        binding.menuLocationPermission.setOnClickListener { openAppSettings() }
        binding.tvVersion.text = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        binding.buttonLogout.setOnClickListener {
            AlertDialog.Builder(requireContext()).setMessage("로그아웃할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("로그아웃") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    requireActivity().finish()
                }.show()
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return
        listeners += db.collection("users").document(uid).addSnapshotListener { document, error ->
            val b = _binding ?: return@addSnapshotListener
            if (error != null) {
                toast("프로필을 불러오지 못했어요. 네트워크를 확인해주세요.")
                return@addSnapshotListener
            }
            if (document == null || !document.exists()) return@addSnapshotListener
            b.tvNickname.text = document.getString("nickname")?.ifBlank { "닉네임" } ?: "닉네임"
            b.tvIntroduction.text = document.getString("bio") ?: document.getString("intro") ?: ""
            ProfileImageLoader.load(b.imgProfile,
                document.getString("profileImageUrl") ?: document.getString("profileImage") ?: "")
        }
        listeners += db.collection("groups").whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                val b = _binding ?: return@addSnapshotListener
                if (error == null && snapshot != null) b.tvJoinedCount.text = snapshot.documents.count {
                    it.getLong("status")?.toInt() != GroupData.STATUS_CANCELED
                }.toString()
                else b.tvJoinedCount.text = "-"
            }
        listeners += db.collection("groups").whereEqualTo("hostId", uid)
            .addSnapshotListener { snapshot, error ->
                val b = _binding ?: return@addSnapshotListener
                b.tvCreatedCount.text = if (error == null && snapshot != null) snapshot.size().toString() else "-"
            }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        val b = _binding ?: return
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        b.tvLocationPermission.text = when {
            fine == PackageManager.PERMISSION_GRANTED -> "정확한 위치 허용"
            coarse == PackageManager.PERMISSION_GRANTED -> "대략적인 위치 허용"
            else -> "허용 안 됨"
        }
    }

    private fun openInfo(page: String) {
        findNavController().navigate(R.id.appInfoFragment, Bundle().apply { putString("page", page) })
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${requireContext().packageName}")))
    }

    override fun onStop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        super.onStop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun toast(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
