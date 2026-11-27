package com.do_it

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.TimeUnit

// 로그인 화면 액티비티
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var verificationId = ""
    private var requestedPhone = ""
    private var instantCredential: PhoneAuthCredential? = null
    private var imageUri: Uri? = null

    // 프로필 사진 선택
    private val selectImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        if (it != null) {
            imageUri = it
            binding.imageProfile.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth.setLanguageCode("ko")
        auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        // 여러 버튼들에 대한 클릭이벤트 처리기 등록
        setClickListeners()

        // 처음 화면을 보여줌
        showInitialScreen()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // 여러 버튼들에 대한 클릭이벤트 처리기 등록
    private fun setClickListeners() {
        binding.buttonLogin.setOnClickListener { requestCode() }
        binding.buttonConfirmLogin.setOnClickListener { loginWithPhone() }
        binding.buttonSelectImage.setOnClickListener { selectImage.launch("image/*") }
        binding.buttonSaveProfile.setOnClickListener { createProfile() }
        binding.buttonBackLogin.setOnClickListener {
            auth.signOut()
            showLoginScreen()
        }
        binding.buttonLogout.setOnClickListener { logout() }
    }

    // 처음 화면을 보여줌
    private fun showInitialScreen() {
        val user = auth.currentUser

        if (user == null) {
            showLoginScreen()
            return
        }

        if (user.phoneNumber.isNullOrBlank()) {
            auth.signOut()
            showLoginScreen()
        } else {
            showHomeScreen(user)
        }
    }

    // 인증번호 요청
    private fun requestCode() {
        val phone = binding.editLoginPhone.text.toString()
            .trim().replace("-", "").replace(" ", "")

        // 휴대폰 번호 유효성 검사
        if (!phone.matches(Regex("010[0-9]{8}"))) {
            toast("올바른 휴대폰 번호를 입력하세요.")
            return
        }

        val phoneNumber = "+82" + phone.substring(1)

        verificationId = ""
        instantCredential = null
        requestedPhone = phone

        binding.editLoginCode.text.clear()
        binding.textVerificationStatus.text = "인증번호를 요청하고 있습니다..."

        setButtonsEnabled(false)

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(0L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                // 즉시 인증 완료
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    instantCredential = credential
                    binding.textVerificationStatus.text = "로그인 버튼을 눌러주세요."
                    setButtonsEnabled(true)
                }

                // 인증 요청 실패
                override fun onVerificationFailed(e: FirebaseException) {
                    verificationId = ""
                    instantCredential = null
                    binding.textVerificationStatus.text = "인증번호 요청 실패"
                    setButtonsEnabled(true)
                    showError(e)
                }

                // 인증번호 발송
                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id

                    binding.textVerificationStatus.text = "인증번호를 입력해주세요."

                    setButtonsEnabled(true)
                    toast("인증번호 발송 요청이 완료되었습니다.")
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // 휴대폰 번호로 로그인
    private fun loginWithPhone() {
        val currentUser = auth.currentUser

        // 회원정보 조회에 실패한 경우 다시 조회
        if (currentUser != null && !currentUser.phoneNumber.isNullOrBlank()) {
            showHomeScreen(currentUser)
            return
        }

        val phone = binding.editLoginPhone.text.toString()
            .trim().replace("-", "").replace(" ", "")

        val code = binding.editLoginCode.text.toString().trim()

        if (phone != requestedPhone) {
            toast("인증번호를 다시 요청해주세요.")
            return
        }

        val credential: PhoneAuthCredential
        val savedCredential = instantCredential

        if (savedCredential != null) {
            credential = savedCredential
        } else {
            if (verificationId.isBlank()) {
                toast("인증번호를 먼저 요청하세요.")
                return
            }

            if (!code.matches(Regex("[0-9]{6}"))) {
                toast("인증번호 6자리를 입력하세요.")
                return
            }

            credential = PhoneAuthProvider.getCredential(verificationId, code)
        }

        setButtonsEnabled(false)
        binding.textVerificationStatus.text = "인증 중입니다..."

        // Firebase에 로그인 요청
        val task = auth.signInWithCredential(credential)

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                instantCredential = null
                binding.textVerificationStatus.text = "인증 실패. 다시 시도해주세요."
                setButtonsEnabled(true)
                showError(it.exception)
                return@addOnCompleteListener
            }

            val user = auth.currentUser

            if (user == null) {
                binding.textVerificationStatus.text = "사용자 정보 조회 실패"
                setButtonsEnabled(true)
                toast("사용자 정보를 가져오지 못함")
                return@addOnCompleteListener
            }

            verificationId = ""
            requestedPhone = ""
            instantCredential = null

            binding.textVerificationStatus.text = "인증 완료"

            showHomeScreen(user)
        }
    }

    // 프로필 등록
    private fun createProfile() {
        val name = binding.editProfileName.text.toString().trim()
        val nickname = binding.editProfileNickname.text.toString().trim()
        val bio = binding.editProfileBio.text.toString().trim()

        // 입력한 내용의 유효성 검사
        if (!validateProfile(name, nickname)) return

        val user = auth.currentUser

        if (user == null || user.phoneNumber.isNullOrBlank()) {
            toast("사용자 정보를 가져오지 못함")
            return
        }

        setButtonsEnabled(false)

        val uri = imageUri

        // 프로필 사진이 없는 경우
        if (uri == null) {
            saveProfile(user, name, nickname, bio, "")
            return
        }

        // Firebase Storage에 프로필 사진 저장
        val imageRef = FirebaseStorage.getInstance()
            .reference.child("profiles/${user.uid}/profile")

        val task = imageRef.putFile(uri)

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                setButtonsEnabled(true)
                showError(it.exception)
            } else {
                // 저장된 이미지의 주소 가져오기
                val urlTask = imageRef.downloadUrl

                urlTask.addOnCompleteListener { result ->
                    if (!result.isSuccessful) {
                        setButtonsEnabled(true)
                        showError(result.exception)
                    } else {
                        saveProfile(
                            user, name, nickname, bio,
                            result.result.toString()
                        )
                    }
                }
            }
        }
    }

    // Firestore에 회원정보 저장
    private fun saveProfile(
        user: FirebaseUser,
        name: String,
        nickname: String,
        bio: String,
        imageUrl: String
    ) {
        val newUser = hashMapOf(
            "id" to user.uid,
            "phoneNumber" to user.phoneNumber,
            "name" to name,
            "nickname" to nickname,
            "bio" to bio,
            "profileImageUrl" to imageUrl,
            "createdAt" to (user.metadata?.creationTimestamp
                ?: System.currentTimeMillis())
        )

        val task = db.collection("users")
            .document(user.uid)
            .set(newUser, SetOptions.merge())

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                setButtonsEnabled(true)
                showError(it.exception)
                return@addOnCompleteListener
            }

            toast("프로필 등록 완료")
            showHomeScreen(user)
        }
    }

    // 로그아웃
    private fun logout() {
        auth.signOut()
        showLoginScreen()
        toast("로그아웃되었습니다.")
    }

    // 로그인 화면 보여주기
    private fun showLoginScreen() {
        verificationId = ""
        requestedPhone = ""
        instantCredential = null
        imageUri = null

        binding.editLoginPhone.text.clear()
        binding.editLoginCode.text.clear()

        binding.editProfileName.text.clear()
        binding.editProfileNickname.text.clear()
        binding.editProfileBio.text.clear()

        binding.imageProfile.setImageResource(android.R.drawable.ic_menu_camera)

        binding.textVerificationStatus.text = "휴대폰 번호를 입력해주세요."

        binding.layoutLogin.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
        binding.layoutHome.visibility = View.GONE

        setButtonsEnabled(true)
    }

    // 프로필 등록 화면 보여주기
    private fun showProfileScreen() {
        binding.layoutLogin.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE
        binding.layoutHome.visibility = View.GONE

        setButtonsEnabled(true)
    }

    // 홈화면으로 이동
    private fun showHomeScreen(user: FirebaseUser) {
        binding.layoutLogin.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
        binding.layoutHome.visibility = View.GONE

        setButtonsEnabled(false)

        binding.textVerificationStatus.text = "인증 완료. 회원정보를 확인하고 있습니다..."

        val task = db.collection("users")
            .document(user.uid)
            .get(Source.SERVER)

        task.addOnCompleteListener {
            if (!it.isSuccessful) {
                binding.textVerificationStatus.text = "회원정보 조회 실패"
                setButtonsEnabled(true)
                showError(it.exception)
                return@addOnCompleteListener
            }

            val document = it.result

            val name = document.getString("name")
            val nickname = document.getString("nickname")

            // 회원정보가 없으면 프로필 등록 화면으로 이동
            if (!document.exists() || name.isNullOrBlank() || nickname.isNullOrBlank()) {
                binding.editProfileName.setText(name ?: "")
                binding.editProfileNickname.setText(nickname ?: "")
                binding.editProfileBio.setText(document.getString("bio") ?: "")

                showProfileScreen()
            } else {
                binding.textVerificationStatus.text = "로그인 성공"
                toast("로그인 성공")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    // 프로필 입력값 유효성 검사
    private fun validateProfile(name: String, nickname: String): Boolean {
        if (name.isBlank()) {
            toast("이름을 입력하세요.")
            return false
        }

        if (nickname.isBlank()) {
            toast("닉네임을 입력하세요.")
            return false
        }

        return true
    }

    // 버튼들의 활성화 및 비활성화 설정
    private fun setButtonsEnabled(enabled: Boolean) {
        val verified = !auth.currentUser?.phoneNumber.isNullOrBlank()

        binding.buttonLogin.isEnabled = enabled && !verified

        binding.buttonConfirmLogin.isEnabled = enabled &&
                (verified || verificationId.isNotBlank() || instantCredential != null)

        binding.buttonSaveProfile.isEnabled = enabled && verified
        binding.buttonSelectImage.isEnabled = enabled
        binding.buttonBackLogin.isEnabled = enabled
        binding.buttonLogout.isEnabled = enabled

        binding.editLoginPhone.isEnabled = enabled && !verified

        binding.editLoginCode.isEnabled = enabled && !verified &&
                verificationId.isNotBlank() && instantCredential == null

        binding.editProfileName.isEnabled = enabled
        binding.editProfileNickname.isEnabled = enabled
        binding.editProfileBio.isEnabled = enabled

        if (verified) {
            binding.buttonConfirmLogin.text = "다시 확인"
        } else {
            binding.buttonConfirmLogin.text = "로그인"
        }
    }

    // 오류 상황에서 토스트를 띄우고 Log로 남김
    private fun showError(error: Exception?) {
        Log.e("FirebaseAuthExample", "Authentication error", error)
        toast(error?.localizedMessage ?: "인증 처리 중 오류가 발생했습니다.", true)
    }

    // 토스트 띄워줌
    private fun toast(message: String, long: Boolean = false) {
        Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}