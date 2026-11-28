package com.do_it

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FragmentEditProfileBinding
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null
    private var imageUrl = ""
    private var loaded = false
    private var saving = false
    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val b = _binding
        if (uri != null && b != null) {
            if (ProfileImageLoader.showSelected(b.imageProfile, uri)) imageUri = uri
            else toast("사진을 불러오지 못했어요. 다른 사진을 선택해주세요.")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uid = auth.currentUser?.uid ?: run {
            toast("로그인이 필요해요.")
            findNavController().popBackStack()
            return
        }
        setEnabled(false)
        binding.buttonSelectImage.setOnClickListener { selectImage.launch("image/*") }
        binding.buttonSaveProfile.setOnClickListener { saveProfile(uid) }
        val currentBinding = binding
        db.collection("users").document(uid).get().addOnCompleteListener { task ->
            if (_binding !== currentBinding) return@addOnCompleteListener
            if (!task.isSuccessful) {
                toast("프로필 조회 실패. 이전 화면에서 다시 시도해주세요.")
                return@addOnCompleteListener
            }
            val document = task.result
            if (!document.exists()) {
                toast("등록된 프로필을 찾지 못했어요.")
                return@addOnCompleteListener
            }
            binding.editProfileName.setText(document.getString("name") ?: "")
            binding.editProfileNickname.setText(document.getString("nickname") ?: "")
            binding.editProfileBio.setText(document.getString("bio") ?: document.getString("intro") ?: "")
            imageUrl = document.getString("profileImageUrl") ?: document.getString("profileImage") ?: ""
            ProfileImageLoader.load(binding.imageProfile, imageUrl)
            loaded = true
            setEnabled(true)
        }
    }

    private fun saveProfile(uid: String) {
        if (!loaded || saving || auth.currentUser?.uid != uid) return
        val name = binding.editProfileName.text.toString().trim()
        val nickname = binding.editProfileNickname.text.toString().trim()
        val bio = binding.editProfileBio.text.toString().trim()
        if (name.isBlank()) { binding.editProfileName.error = "이름을 입력하세요."; return }
        if (nickname.isBlank()) { binding.editProfileNickname.error = "닉네임을 입력하세요."; return }
        saving = true
        setEnabled(false)
        val currentBinding = binding
        val fields = hashMapOf<String, Any>("name" to name, "nickname" to nickname, "bio" to bio)
        fun save(url: String, uploadedPath: String? = null) {
            fields["profileImageUrl"] = url
            db.collection("users").document(uid).set(fields, SetOptions.merge())
                .addOnSuccessListener {
                    if (_binding === currentBinding) {
                        saving = false
                        toast("프로필을 수정했어요.")
                        findNavController().popBackStack()
                    }
                }.addOnFailureListener {
                    if (uploadedPath != null) FirebaseStorage.getInstance().reference.child(uploadedPath).delete()
                    if (_binding === currentBinding) showFailure("프로필을 저장하지 못했어요. 다시 시도해주세요.")
                }
        }
        val uri = imageUri
        if (uri == null) { save(imageUrl); return }
        val path = "profiles/$uid/${UUID.randomUUID()}"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        ref.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception ?: IllegalStateException("사진 업로드 실패")
            ref.downloadUrl
        }.addOnSuccessListener { save(it.toString(), path) }
            .addOnFailureListener {
                ref.delete()
                if (_binding === currentBinding) showFailure("사진 저장 실패. 네트워크와 사진 접근 권한을 확인해주세요.")
            }
    }

    private fun setEnabled(enabled: Boolean) {
        val b = _binding ?: return
        b.buttonSaveProfile.isEnabled = enabled
        b.buttonSelectImage.isEnabled = enabled
        b.editProfileName.isEnabled = enabled
        b.editProfileNickname.isEnabled = enabled
        b.editProfileBio.isEnabled = enabled
        b.buttonSaveProfile.text = if (saving) "저장 중..." else "저장"
    }

    private fun showFailure(message: String) {
        saving = false
        setEnabled(true)
        toast(message)
    }

    private fun toast(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        loaded = false
        imageUri = null
        super.onDestroyView()
    }
}
