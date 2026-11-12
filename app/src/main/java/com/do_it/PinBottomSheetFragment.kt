package com.do_it

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FragmentPinBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PinBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentPinBottomSheetBinding
    private var group: GroupData? = null   // 불러온 모임 정보

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPinBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupId = arguments?.getString("groupId")
        if (groupId == null) return

        // 모임 정보 가져오기
        val task = db.collection("groups").document(groupId).get()
        task.addOnSuccessListener {
            val item = it.toObject(GroupData::class.java)
            if (item == null) return@addOnSuccessListener
            group = item
            showGroup(item)
        }
        task.addOnFailureListener {
            Toast.makeText(context, "모임 정보 불러오기 실패", Toast.LENGTH_SHORT).show()
        }

        // 참여하기 버튼
        binding.btnJoin.setOnClickListener {
            joinGroup()
        }
    }

    // 모임 참여 : groups 문서의 memberIds, memberCount 수정 + members 하위 문서 추가
    private fun joinGroup() {
        val item = group
        if (item == null) return
        val uid = auth.currentUser?.uid
        if (uid == null) return

        if (item.memberIds.contains(uid)) {
            Toast.makeText(context, "이미 참여 중인 모임이에요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (item.status != GroupData.STATUS_RECRUITING) {
            Toast.makeText(context, "모집이 끝난 모임이에요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (item.memberCount >= item.maxMember) {
            Toast.makeText(context, "정원이 다 찼어요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 기존 멤버 목록에 내 uid 추가
        val newMemberIds = item.memberIds.toMutableList()
        newMemberIds.add(uid)

        val ref = db.collection("groups").document(item.id)
        val task = ref.update("memberIds", newMemberIds, "memberCount", item.memberCount + 1)
        task.addOnSuccessListener {
            ref.collection("members").document(uid).set(memberData(
                meetingId = item.id,
                userId = uid,
                name = auth.currentUser?.email ?: "",
                role = memberData.ROLE_MEMBER,
                status = memberData.STATUS_JOINED,
                joinedAt = System.currentTimeMillis()
                )
            )
            Toast.makeText(context, "모임에 참여했어요!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
        task.addOnFailureListener {
            Toast.makeText(context, "참여 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 모임 정보를 화면에 보여줌
    private fun showGroup(item: GroupData) {
        binding.tvTitle.text = item.title
        binding.tvMemberCount.text = "${item.memberCount}/${item.maxMember}명"

        // 이미 참여 중이면 버튼 글자 바꾸기
        if (item.memberIds.contains(auth.currentUser?.uid)) {
            binding.btnJoin.text = "참여 중"
        }

        // 카테고리
        if (item.category.isEmpty()) {
            binding.tvCategory.text = "기타"
        } else {
            binding.tvCategory.text = item.category
        }

        // 모집 상태
        binding.tvStatus.text = when (item.status) {
            GroupData.STATUS_RECRUITING -> "모집 중"
            GroupData.STATUS_CONFIRMED -> "모집 확정"
            else -> "종료"
        }

        // 일시
        if (item.dateTime == 0L) {
            binding.tvDate.text = "일시 미정"
        } else {
            binding.tvDate.text = DateFormat.format("yyyy.MM.dd EEEE HH:mm", item.dateTime)
        }

        // 상세 설명
        binding.tvDescription.text = item.description

        // 장소
        if (item.place.isEmpty()) {
            binding.tvPlace.text = "동양미래대학교 인근"
        } else {
            binding.tvPlace.text = item.place
        }

        // 방장 이름은 users 컬렉션에서 가져오기
        if (item.hostId.isNotEmpty()) {
            val userTask = db.collection("users").document(item.hostId).get()
            userTask.addOnSuccessListener {
                val user = it.toObject(userData::class.java)
                if (user == null) {
                    binding.tvHost.text = "방장"
                } else {
                    binding.tvHost.text = "${user.email} (방장)"
                }
            }
        }
    }
}
