package com.do_it

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.FragmentFriendBinding

class FriendFragment : Fragment() {

    private lateinit var binding: FragmentFriendBinding
    private lateinit var friendAdapter: friendListAdapter
    private lateinit var invitationAdapter: friendInvitationListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 더미 데이터 (친구 데이터가 생기기 전 화면 확인용)
        val friendList = mutableListOf(
            userData(
                id = "u1",
                email = "daeun@test.com",
                nickname = "정다은"
            ),
            userData(
                id = "u2",
                email = "minsu@test.com",
                nickname = "김민수"
            )
        )

        friendAdapter = friendListAdapter(
            { item ->
                val intent: Intent = Intent(requireContext(), PersonalChatActivity::class.java)
                intent.putExtra("friendId", item.id)
                startActivity(intent)
            },
            { item ->
                // 친구 상세 화면 (아직 없음)
            },
            { item ->                                                                    // ← 추가
                // 길게 클릭 : 친구 삭제 / 신고 선택                                         // ← 추가
                val builder = AlertDialog.Builder(requireContext())                       // ← 추가
                builder.setTitle(item.nickname)                                           // ← 추가
                builder.setItems(arrayOf("친구 삭제", "신고"), object: DialogInterface.OnClickListener {   // ← 추가
                    override fun onClick(dialog: DialogInterface?, which: Int) {          // ← 추가
                        if (which == 0) {
                            // 친구 삭제 : 한 번 더 물어보고 삭제
                            val builder2 = AlertDialog.Builder(requireContext())
                            builder2.setTitle("친구 삭제")
                            builder2.setMessage("${item.nickname}님을 친구에서 삭제할까요?")
                            builder2.setPositiveButton("삭제", object: DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    friendAdapter.deleteFriend(item)
                                }
                            })
                            builder2.setNegativeButton("취소", null)
                            builder2.show()
                        } else {
                            // 신고 : 신고 내용을 입력받고 신고
                            val editText = EditText(requireContext())
                            editText.hint = "신고 내용을 적어주세요"

                            val builder3 = AlertDialog.Builder(requireContext())
                            builder3.setTitle("신고")
                            builder3.setMessage("${item.nickname}님을 신고할까요?")
                            builder3.setView(editText)
                            builder3.setPositiveButton("신고", object: DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, which: Int) {
                                    friendAdapter.reportFriend(item, editText.text.toString().trim())
                                }
                            })
                            builder3.setNegativeButton("취소", null)
                            builder3.show()   // 3단계에서 바꿈
                        }                                                                // ← 추가
                    }                                                                     // ← 추가
                })                                                                        // ← 추가
                builder.setNegativeButton("취소", null)                                   // ← 추가
                builder.show()                                                            // ← 추가
            },                                                                            // ← 추가
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        )

        binding.rvMyFriends.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvMyFriends.adapter = friendAdapter

//        friendAdapter.setItems(friendList)
/////////////////////////////////////////////////////////////////////////////////
        // 더미 데이터 (친구 요청이 생기기 전 화면 확인용)
        val friendInvitationList = mutableListOf(
            userData(
                id = "u3",
                email = "jiho@test.com",
                nickname = "이지호"
            ),
            userData(
                id = "u4",
                email = "seoyeon@test.com",
                nickname = "박서연"
            )
        )

        invitationAdapter = friendInvitationListAdapter(
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
            { friendAdapter.loadItems() }
        )

        binding.rvFriendInvitations.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvFriendInvitations.adapter = invitationAdapter

//        invitationAdapter.setItems(friendInvitationList)
    }

     //실제 Firestore 데이터를 쓸 때 주석 해제 (켜 두면 빈 목록이 더미 데이터를 덮어씀)
     override fun onResume() {
         super.onResume()
         friendAdapter.loadItems()
         invitationAdapter.loadItems()
     }
}
