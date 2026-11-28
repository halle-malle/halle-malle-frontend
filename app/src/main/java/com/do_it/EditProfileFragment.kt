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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private var loaded = false
    private var saving = false

    private val selectImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        val currentBinding = _binding

        if (uri != null && currentBinding != null) {
            if (ProfileImageLoader.showSelected(currentBinding.imageProfile, uri)) {
                imageUri = uri
            } else {
                toast("사진을 불러오지 못했어요. 다른 사진을 선택해주세요.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(
            inflater, container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            toast("로그인이 필요해요.")
            findNavController().popBackStack()
            return
        }

        setEnabled(false)

        binding.buttonSelectImage.setOnClickListener {
            selectImage.launch("image/*")
        }

        binding.buttonSaveProfile.setOnClickListener {
            saveProfile(uid)
        }

        loadProfile(uid)
    }

    private fun loadProfile(uid: String) {
        val currentBinding = binding

        db.collection("users").document(uid).get()
            .addOnCompleteListener { task ->
                if (_binding !== currentBinding) {
                    return@addOnCompleteListener
                }

                if (!task.isSuccessful) {
                    toast("프로필 조회 실패. 이전 화면에서 다시 시도해주세요.")
                    return@addOnCompleteListener
                }

                val document = task.result

                if (!document.exists()) {
                    toast("등록된 프로필을 찾지 못했어요.")
                    return@addOnCompleteListener
                }

                currentBinding.editProfileName.setText(
                    document.getString("name") ?: ""
                )
                currentBinding.editProfileNickname.setText(
                    document.getString("nickname") ?: ""
                )
                currentBinding.editProfileBio.setText(
                    document.getString("bio")
                        ?: document.getString("intro")
                        ?: ""
                )

                val imageUrl = document.getString("profileImageUrl")
                    ?: document.getString("profileImage")
                    ?: ""

                ProfileImageLoader.load(
                    currentBinding.imageProfile, imageUrl
                )

                loaded = true
                setEnabled(true)
            }
    }

    private fun saveProfile(uid: String) {
        if (!loaded || saving || auth.currentUser?.uid != uid) {
            return
        }

        val name = binding.editProfileName.text.toString().trim()
        val nickname = binding.editProfileNickname.text.toString().trim()
        val bio = binding.editProfileBio.text.toString().trim()

        if (name.isBlank()) {
            binding.editProfileName.error = "이름을 입력하세요."
            return
        }

        if (nickname.isBlank()) {
            binding.editProfileNickname.error = "닉네임을 입력하세요."
            return
        }

        saving = true
        setEnabled(false)

        val currentBinding = binding
        val uri = imageUri

        if (uri == null) {
            updateProfile(
                uid, name, nickname, bio, null, currentBinding
            )
            return
        }

        uploadProfileImage(uid, name, nickname, bio, uri, currentBinding)
    }

    private fun uploadProfileImage(
        uid: String,
        name: String,
        nickname: String,
        bio: String,
        uri: Uri,
        currentBinding: FragmentEditProfileBinding
    ) {
        val imageRef = FirebaseStorage.getInstance().reference
            .child("profiles/$uid/${System.currentTimeMillis()}")

        val task = imageRef.putFile(uri)

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                if (_binding === currentBinding) {
                    showFailure("사진을 저장하지 못했어요. 다시 시도해주세요.")
                }
                return@addOnCompleteListener
            }

            val urlTask = imageRef.downloadUrl

            urlTask.addOnCompleteListener { result ->
                if (!result.isSuccessful || auth.currentUser?.uid != uid) {
                    imageRef.delete()

                    if (_binding === currentBinding) {
                        showFailure("사진 주소를 가져오지 못했어요. 다시 시도해주세요.")
                    }
                } else {
                    updateProfile(
                        uid,
                        name,
                        nickname,
                        bio,
                        result.result.toString(),
                        currentBinding,
                        imageRef
                    )
                }
            }
        }
    }

    private fun updateProfile(
        uid: String,
        name: String,
        nickname: String,
        bio: String,
        newImageUrl: String?,
        currentBinding: FragmentEditProfileBinding,
        uploadedImage: StorageReference? = null
    ) {
        if (auth.currentUser?.uid != uid) {
            uploadedImage?.delete()

            if (_binding === currentBinding) {
                showFailure("로그인 상태가 변경됐어요. 다시 로그인해주세요.")
            }
            return
        }

        val fields = hashMapOf<String, Any>(
            "name" to name,
            "nickname" to nickname,
            "bio" to bio
        )

        if (newImageUrl != null) {
            fields["profileImageUrl"] = newImageUrl
        }

        val task = db.collection("users")
            .document(uid)
            .update(fields)

        task.addOnSuccessListener {
            if (_binding === currentBinding && isAdded) {
                saving = false
                toast("프로필을 수정했어요.")
                findNavController().popBackStack()
            }
        }

        task.addOnFailureListener {
            uploadedImage?.delete()

            if (_binding === currentBinding) {
                showFailure("프로필을 저장하지 못했어요. 다시 시도해주세요.")
            }
        }
    }

    private fun setEnabled(enabled: Boolean) {
        val currentBinding = _binding ?: return

        currentBinding.buttonSaveProfile.isEnabled = enabled
        currentBinding.buttonSelectImage.isEnabled = enabled
        currentBinding.editProfileName.isEnabled = enabled
        currentBinding.editProfileNickname.isEnabled = enabled
        currentBinding.editProfileBio.isEnabled = enabled
        currentBinding.buttonSaveProfile.text =
            if (saving) "저장 중..." else "저장"
    }

    private fun showFailure(message: String) {
        saving = false
        setEnabled(true)
        toast(message)
    }

    private fun toast(message: String) {
        if (isAdded) {
            Toast.makeText(
                requireContext(), message, Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        loaded = false
        saving = false
        imageUri = null
        super.onDestroyView()
    }
}