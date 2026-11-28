package com.do_it

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.ActivityGroupChatBinding
import com.do_it.databinding.ActivityGroupDetailBinding
import com.do_it.databinding.ActivityMainBinding
import com.do_it.databinding.FragmentMypageBinding

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var adapter: detailMemberAdapter
    private val memberList = mutableListOf<memberData>()   // 함께하는 멤버 목록

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return
        if (!::adapter.isInitialized) return

        db.collection("users").document(uid).collection("friends")
            .whereEqualTo("status", 1)
            .addSnapshotListener(this) { snapshot, _ ->
                if (snapshot != null && auth.currentUser?.uid == uid) {
                    adapter.updateFriends(snapshot.documents.map { it.id }.toSet())
                }
            }
    }

    // 모임 삭제 : 멤버, 채팅 문서를 지우고 모임 문서도 삭제
    private fun deleteGroup(groupId: String) {
        val ref = db.collection("groups").document(groupId)

        ref.collection("members").get().addOnSuccessListener {
            for (document in it) {
                document.reference.delete()
            }
        }
        ref.collection("chats").get().addOnSuccessListener {
            for (document in it) {
                document.reference.delete()
            }
        }

        val task = ref.delete()
        task.addOnSuccessListener {
            Toast.makeText(baseContext, "모임이 삭제되었어요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        task.addOnFailureListener {
            Toast.makeText(baseContext, "삭제 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 모임 탈퇴 : members에서 내 문서를 지우고, 모임의 memberIds와 memberCount 수정
    private fun leaveGroup(groupId: String, uid: String, item: GroupData) {
        val ref = db.collection("groups").document(groupId)

        ref.collection("members").document(uid).delete()

        val task = ref.update(
            "memberIds", item.memberIds - uid,
            "memberCount", item.memberCount - 1
        )
        task.addOnSuccessListener {
            Toast.makeText(baseContext, "모임에서 나왔어요.", Toast.LENGTH_SHORT).show()
            finish()
        }
        task.addOnFailureListener {
            Toast.makeText(baseContext, "탈퇴 실패", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        NotificationHistory.bind(this, binding.btnNotification)

        val rIntent = getIntent()
        val groupId = rIntent.getStringExtra("groupId")   // 홈에서 넘겨준 모임 id
        if (groupId == null) {
            finish()
            return
        }

        binding.bckBtnFrmGroupDetail.setOnClickListener{
            finish()
        }

        binding.btnGroupChat.setOnClickListener {
            val intent: Intent = Intent(this, GroupChatActivity::class.java)
            intent.putExtra("groupId", groupId)
            startActivity(intent)
        }
        binding.tvNewCount.visibility = View.GONE   // 새 메시지 수는 아직 없어서 숨김

        adapter = detailMemberAdapter(memberList) { member ->
            val myId = auth.currentUser?.uid ?: return@detailMemberAdapter
            val ref = db.collection("users").document(member.userId).collection("friends").document(myId)
            ref.get().addOnSuccessListener {
                if (it.exists()) {   // 이미 요청했거나 이미 친구
                    Toast.makeText(baseContext, "이미 요청했거나 친구예요.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                ref.set(friendData(userId = myId, status = 0, createdAt = System.currentTimeMillis()))
                    .addOnSuccessListener { Toast.makeText(baseContext, "친구 요청을 보냈어요.", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(baseContext, "요청 실패", Toast.LENGTH_SHORT).show() }
            }
        }
        binding.rvMember.layoutManager = LinearLayoutManager(this)
        binding.rvMember.adapter = adapter
        binding.rvMember.addItemDecoration(DividerItemDecoration(this,
            LinearLayoutManager.VERTICAL))

        // 모임 정보 가져오기
        val task = db.collection("groups").document(groupId).get()
        task.addOnSuccessListener {
            val item = it.toObject(GroupData::class.java)
            if (item == null) return@addOnSuccessListener

            // 카테고리 이모지 모임 이름 앞에 붙이기
            val emoji = when (item.category) {
                "운동" -> "🏸"
                "스터디" -> "💻"
                "맛집" -> "🍕"
                "취미" -> "🎸"
                else -> "💬"
            }
            binding.tvGroupName.text = emoji + " " + item.title
            binding.tvCategory.text = if (item.category.isEmpty()) "기타" else item.category
            // 모집 상태
            val started = item.dateTime < System.currentTimeMillis()
            val past = item.status == GroupData.STATUS_FINISHED || item.dateTime + GroupData.END_TIME < System.currentTimeMillis()
            if (item.status == GroupData.STATUS_CANCELED) {
                binding.tvMeetupState.text = "모임 취소"
                binding.btnGroupChat.visibility = View.GONE
            } else if (past) {
                binding.tvMeetupState.text = "지난 모임"
                binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_past)
            } else if (started) {
                binding.tvMeetupState.text = "모임 중"
                binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_ongoing)
            } else if (item.status == GroupData.STATUS_CONFIRMED) {
                binding.tvMeetupState.text = "모집 확정"
                binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_confirmed)
            } else {
                binding.tvMeetupState.text = "모집 중"
                binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_recruiting)
            }
            binding.tvDate.text = if (item.dateTime == 0L) "일시 미정" else DateFormat.format("yyyy.MM.dd EEEE HH:mm", item.dateTime)
            binding.tvPlace.text = if (item.place.isEmpty()) "동양미래대학교 인근" else item.place
            binding.tvMember.text = "${item.memberCount}/${item.maxMember}명"
            binding.textView22.text = "함께하는 멤버 (${item.memberCount})"

            // 방장 / 참여자 버튼
            val uid = auth.currentUser?.uid
            if (!started && uid != null &&
                (item.hostId == uid || uid in item.memberIds) &&
                item.status in listOf(GroupData.STATUS_RECRUITING, GroupData.STATUS_CONFIRMED)
            ) {
                binding.btnCancelVote.visibility = View.VISIBLE
                binding.btnCancelVote.setOnClickListener {
                    val submitVote: (Boolean) -> Unit = { approve ->
                        binding.btnCancelVote.isEnabled = false
                        FirebaseFunctions.getInstance("asia-northeast3")
                            .getHttpsCallable("castGroupCancellationVote")
                            .call(mapOf("groupId" to groupId, "approve" to approve))
                            .addOnCompleteListener(this) { vote ->
                                if (vote.isSuccessful) {
                                    val result = vote.result.data as? Map<*, *>
                                    binding.btnCancelVote.visibility = View.GONE
                                    if (result?.get("canceled") == true) {
                                        binding.tvMeetupState.text = "모임 취소"
                                        binding.btnDeleteGroup.visibility = View.GONE
                                        binding.btnGroupChat.visibility = View.GONE
                                        Toast.makeText(this, "모임이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val yes = result?.get("yes")
                                        val required = result?.get("required")
                                        Toast.makeText(this, "투표 완료 · 찬성 ${yes}표 / 필요 ${required}표", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val error = vote.exception as? FirebaseFunctionsException
                                    binding.btnCancelVote.isEnabled = true
                                    if (error?.code == FirebaseFunctionsException.Code.ALREADY_EXISTS) {
                                        binding.btnCancelVote.visibility = View.GONE
                                    }
                                    Toast.makeText(this, error?.message ?: "투표하지 못했어요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    AlertDialog.Builder(this)
                        .setTitle("모임 취소 투표")
                        .setMessage("방장을 포함한 전체 참가자 과반이 찬성하면 모임이 취소됩니다. 투표는 한 번만 할 수 있어요.")
                        .setPositiveButton("찬성") { _, _ -> submitVote(true) }
                        .setNegativeButton("반대") { _, _ -> submitVote(false) }
                        .setNeutralButton("닫기", null)
                        .show()
                }
            }

            if (item.status == GroupData.STATUS_CANCELED) {
                binding.btnDeleteGroup.visibility = View.GONE
            } else if (started && !past && item.hostId == uid) {
                binding.btnDeleteGroup.text = "모임 종료"
                binding.btnDeleteGroup.visibility = View.VISIBLE
                // 모임 종료 버튼
                binding.btnDeleteGroup.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("모임 종료")
                    builder.setMessage("모임을 종료할까요? 종료하면 지난 모임으로 옮겨져요.")
                    builder.setPositiveButton("종료", object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            val finishTask = db.collection("groups").document(groupId).update("status", GroupData.STATUS_FINISHED)
                            finishTask.addOnSuccessListener {
                                Toast.makeText(baseContext, "모임이 종료되었어요.", Toast.LENGTH_SHORT).show()
                                binding.tvMeetupState.text = "지난 모임"
                                binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_past)
                                binding.btnDeleteGroup.visibility = View.GONE
                            }
                            finishTask.addOnFailureListener {
                                Toast.makeText(baseContext, "종료 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                    builder.setNegativeButton("취소", null)
                    builder.show()
                }
            } else if (!started && item.hostId == uid) {
                // 모임 확정 버튼 : 모집 중일 때만 보이기
                if (item.status == GroupData.STATUS_RECRUITING) {
                    binding.btnConfirmGroup.visibility = View.VISIBLE
                    binding.btnConfirmGroup.setOnClickListener {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("모임 확정")
                        builder.setMessage("모임을 확정할까요? 확정하면 더 이상 참여자를 받지 않고, 참여자도 나갈 수 없어요.")
                        builder.setPositiveButton("확정", object: DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                val confirmTask = db.collection("groups").document(groupId).update("status", GroupData.STATUS_CONFIRMED)
                                confirmTask.addOnSuccessListener {
                                    Toast.makeText(baseContext, "모임이 확정되었어요.", Toast.LENGTH_SHORT).show()
                                    binding.tvMeetupState.text = "모집 확정"
                                    binding.tvMeetupState.setBackgroundResource(R.drawable.bg_badge_confirmed)
                                    binding.btnConfirmGroup.visibility = View.GONE
                                }
                                confirmTask.addOnFailureListener {
                                    Toast.makeText(baseContext, "확정 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                        builder.setNegativeButton("취소", null)
                        builder.show()
                    }
                }

                binding.btnDeleteGroup.text = "모임 삭제"
                binding.btnDeleteGroup.visibility = View.VISIBLE
                // 모임 삭제 버튼 : 한 번 더 물어보고 삭제
                binding.btnDeleteGroup.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("모임 삭제")
                    builder.setMessage("모임을 삭제할까요? 삭제하면 되돌릴 수 없어요.")
                    builder.setPositiveButton("삭제", object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            deleteGroup(groupId)
                        }
                    })
                    builder.setNegativeButton("취소", null)
                    builder.show()
                }
            } else if (!started && item.status == GroupData.STATUS_RECRUITING && uid != null && item.memberIds.contains(uid)) {
                binding.btnDeleteGroup.text = "모임 탈퇴"
                binding.btnDeleteGroup.visibility = View.VISIBLE
                // 모임 탈퇴 버튼 (모집 중일 때만)
                binding.btnDeleteGroup.setOnClickListener {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("모임 탈퇴")
                    builder.setMessage("모임에서 나갈까요?")
                    builder.setPositiveButton("탈퇴", object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            leaveGroup(groupId, uid, item)
                        }
                    })
                    builder.setNegativeButton("취소", null)
                    builder.show()
                }
            }

            // 방장 이름
            db.collection("users").document(item.hostId).get().addOnSuccessListener {
                val user = it.toObject(userData::class.java)
                if (user != null) {
                    binding.tv.text = user.email
                } else {
                    binding.tv.text = "알 수 없음"   // users에 회원 정보가 없는 경우
                }
            }
        }
        task.addOnFailureListener {
            Toast.makeText(baseContext, "모임 정보 불러오기 실패", Toast.LENGTH_SHORT).show()
        }

        // 멤버 목록 가져오기 : members 문서마다 users에서 이름, 한줄소개 찾기
        db.collection("groups").document(groupId).collection("members").get().addOnSuccessListener {
            for (document in it) {
                val member = document.toObject(memberData::class.java)
                db.collection("users").document(member.userId).get().addOnSuccessListener {
                    val user = it.toObject(userData::class.java)
                    var name = member.name   // 참여할 때 저장해 둔 이메일
                    var intro = ""
                    if (user != null) {
                        name = user.email
                        intro = user.intro
                    }
                    if (name.isEmpty()) {
                        name = "알 수 없음"
                    }
                    memberList.add(memberData(
                        userId = member.userId,
                        name = name,
                        intro = intro,
                        role = member.role,
                        status = member.status
                    ))
                    memberList.sortBy { it.role }   // 방장이 맨 위로
                    adapter.notifyDataSetChanged()
                }
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}