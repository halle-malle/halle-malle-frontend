package com.do_it

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
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

        adapter = detailMemberAdapter(memberList)
        binding.rvMember.layoutManager = LinearLayoutManager(this)
        binding.rvMember.adapter = adapter
        binding.rvMember.addItemDecoration(DividerItemDecoration(this,
            LinearLayoutManager.VERTICAL))

        // 모임 정보 가져오기
        val task = db.collection("groups").document(groupId).get()
        task.addOnSuccessListener {
            val item = it.toObject(GroupData::class.java)
            if (item == null) return@addOnSuccessListener

            binding.tvGroupName.text = item.title
            binding.tvCategory.text = if (item.category.isEmpty()) "기타" else item.category
            binding.tvMeetupState.text = when (item.status) {
                GroupData.STATUS_RECRUITING -> "모집 중"
                GroupData.STATUS_CONFIRMED -> "모임 확정"
                else -> "종료"
            }
            binding.tvMyState.text = if (item.memberIds.contains(auth.currentUser?.uid)) "참여 중" else "미참여"
            binding.tvDate.text = if (item.dateTime == 0L) "일시 미정" else DateFormat.format("yyyy.MM.dd EEEE HH:mm", item.dateTime)
            binding.tvPlace.text = if (item.place.isEmpty()) "동양미래대학교 인근" else item.place
            binding.tvMember.text = "${item.memberCount}/${item.maxMember}명"
            binding.textView22.text = "함께하는 멤버 (${item.memberCount})"

            // 하단 버튼 : 방장은 모임 삭제, 일반 참여자는 모임 탈퇴, 미참여자는 숨김
            val uid = auth.currentUser?.uid
            if (item.hostId == uid) {
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
            } else if (uid != null && item.memberIds.contains(uid)) {
                binding.btnDeleteGroup.text = "모임 탈퇴"
                binding.btnDeleteGroup.visibility = View.VISIBLE
                // 모임 탈퇴 버튼 : 확정된 모임은 나갈 수 없고, 아니면 한 번 더 물어보고 탈퇴
                binding.btnDeleteGroup.setOnClickListener {
                    if (item.status == GroupData.STATUS_CONFIRMED) {
                        Toast.makeText(baseContext, "확정된 모임은 나갈 수 없어요.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
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