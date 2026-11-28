package com.do_it

import com.bumptech.glide.Glide
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
import com.google.firebase.firestore.Filter

class mypageFragment : Fragment() {
    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!
    private var requestNumber = 0
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
        binding.menuTerms.setOnClickListener {
            findNavController().navigate(R.id.rulesFragment)
        }
        binding.menuPrivacy.setOnClickListener {
            findNavController().navigate(R.id.privacyFragment)
        }
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
        val currentBinding = binding
        val request = ++requestNumber

        val userDocument = db.collection("users").document(uid)
        userDocument.get().addOnCompleteListener { task ->
            if (request != requestNumber || auth.currentUser?.uid != uid) {
                return@addOnCompleteListener
            }
            if (!task.isSuccessful) {
                toast("프로필을 불러오지 못했어요. 네트워크를 확인해주세요.")
                return@addOnCompleteListener
            }

            val document = task.result
            if (!document.exists()) return@addOnCompleteListener

            currentBinding.tvNickname.text = document.getString("nickname")?.ifBlank { "닉네임" } ?: "닉네임"
            currentBinding.tvIntroduction.text = document.getString("bio") ?: document.getString("intro") ?: ""
            val imageUrl = document.getString("profileImageUrl") ?: document.getString("profileImage") ?: ""
            // 이미지가 있으면 프로필 사진 표시
            if(imageUrl.isNotBlank()) {
                val rqMngr = Glide.with(currentBinding.root)
                val rqBuilder = rqMngr.load(imageUrl)
                rqBuilder.placeholder(R.drawable.ic_person)
                rqBuilder.error(R.drawable.ic_person)
                rqBuilder.override(300, 300)
                rqBuilder.into(currentBinding.imgProfile)
            }
        }

        db.collection("groups")
            .where(Filter.or(
                Filter.arrayContains("memberIds", uid),
                Filter.equalTo("hostId", uid)
            ))
            .get()
            .addOnCompleteListener { task ->
                if (request != requestNumber || auth.currentUser?.uid != uid) {
                    return@addOnCompleteListener
                }
                if (!task.isSuccessful) {
                    currentBinding.tvTotalCount.text = "-"
                    currentBinding.tvCreatedCount.text = "-"
                    return@addOnCompleteListener
                }

                val meetings = task.result.documents.filter {
                    it.getLong("status")?.toInt() != GroupData.STATUS_CANCELED
                }
                currentBinding.tvTotalCount.text = meetings.size.toString()
                currentBinding.tvCreatedCount.text = meetings.count {
                    it.getString("hostId") == uid
                }.toString()
            }

        userDocument.collection("friends")
            .whereEqualTo("status", 1)
            .get()
            .addOnCompleteListener { task ->
                if (request != requestNumber || auth.currentUser?.uid != uid) {
                    return@addOnCompleteListener
                }
                currentBinding.tvFriendCount.text =
                    if (task.isSuccessful) task.result.size().toString() else "-"
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

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${requireContext().packageName}")))
    }

    override fun onStop() {
        requestNumber++
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
