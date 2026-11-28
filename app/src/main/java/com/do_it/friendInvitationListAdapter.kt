package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FriendInvitaionListItemBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

class friendInvitationItemViewHolder(
    val binding: FriendInvitaionListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class friendInvitationListAdapter (
    private val toast: (String) -> Unit, // 어답터 클래스 안에서 토스트 창을 띄워야 할 때 호출하는 함수
    private val onAccepted: () -> Unit
) : RecyclerView.Adapter<friendInvitationItemViewHolder>() {

    private var list: MutableList<userData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): friendInvitationItemViewHolder {
        val binding = FriendInvitaionListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return friendInvitationItemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: friendInvitationItemViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvTitle.text = item.nickname
        holder.binding.imageView.setImageResource(R.drawable.ic_person)
        // 이미지가 있으면 프로필 사진 표시
        if(item.profileImageUrl.isNotBlank()) {
            val rqMngr = Glide.with(holder.binding.root)
            val rqBuilder = rqMngr.load(item.profileImageUrl)
            rqBuilder.placeholder(R.drawable.ic_person)
            rqBuilder.error(R.drawable.ic_person)
            rqBuilder.override(300, 300)
            rqBuilder.into(holder.binding.imageView)
        }
        // XML에서 btnChat = 거절, btnDetail = 수락
        holder.binding.btnChat.setOnClickListener {
            reject(item)
        }
        holder.binding.btnDetail.setOnClickListener {
            accept(item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    // 더미 데이터 확인용 : 밖에서 만든 목록을 그대로 보여주기
    fun setItems(items: MutableList<userData>) {
        list = items
        notifyDataSetChanged()
    }

    fun loadItems() {
        // 내 friends 중 받은 요청(status 0)인 문서마다 users에서 보낸 사람 정보 찾기
        val uid = auth.currentUser?.uid ?: return
        val colref: CollectionReference = db.collection("users").document(uid).collection("friends")

        val query: Query = colref.whereEqualTo("status", 0)
        val task = query.get()
        task.addOnSuccessListener {
            list.clear()
            notifyDataSetChanged()
            for (document in it) {
                // 문서 id = 요청 보낸 사람 uid
                db.collection("users").document(document.id).get().addOnSuccessListener {
                    val user = it.toObject(userData::class.java)
                    if (user != null) {
                        list.add(user)
                        notifyDataSetChanged()
                    }
                }
            }
        }
        task.addOnFailureListener {
            toast("읽기 실패")
        }
    }

    // 수락 : 내 쪽 요청을 친구(1)로 바꾸고, 상대방 friends에도 나를 친구로 추가
    // 수락 : 내 쪽 요청을 친구(1)로 바꾸고, 상대방 friends에도 나를 친구로 추가
    private fun accept(item: userData) {
        val uid = auth.currentUser?.uid ?: return
        // update는 문서가 없으면 실패함 (없는 요청을 수락해서 상대방 쪽에 쓰레기 데이터가 생기는 것 방지)
        val task = db.collection("users").document(uid).collection("friends").document(item.id)
            .update("status", 1)
        task.addOnSuccessListener {
            db.collection("users").document(item.id).collection("friends").document(uid).set(friendData(
                userId = uid,
                status = 1,
                createdAt = System.currentTimeMillis()
            )).addOnSuccessListener {   // ← 추가
                onAccepted()             // ← 추가
            }                            // ← 추가
            list.remove(item)
            notifyDataSetChanged()
            toast("친구가 되었어요.")
        }
        task.addOnFailureListener {
            toast("수락 실패")
        }
    }

    // 거절 : 내 쪽 요청 문서 삭제
    private fun reject(item: userData) {
        val uid = auth.currentUser?.uid ?: return
        val task = db.collection("users").document(uid).collection("friends").document(item.id).delete()
        task.addOnSuccessListener {
            list.remove(item)
            notifyDataSetChanged()
        }
        task.addOnFailureListener {
            toast("거절 실패")
        }
    }

}
