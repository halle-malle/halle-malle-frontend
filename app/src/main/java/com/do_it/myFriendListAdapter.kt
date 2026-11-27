package com.do_it

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FriendListItemBinding
import com.do_it.databinding.UpcomingScheduleItemBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

class friendItemViewHolder(
    val binding: FriendListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class friendListAdapter (
    private val onItemClickChat: (userData) -> Unit,
    private val onItemClickDetail: (userData) -> Unit,
    private val toast: (String) -> Unit // 어답터 클래스 안에서 토스트 창을 띄워야 할 때 호출하는 함수

) : RecyclerView.Adapter<friendItemViewHolder>() {

    private var list: MutableList<userData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): friendItemViewHolder {
        val binding = FriendListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return friendItemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: friendItemViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvName.text = item.nickname
        holder.binding.ivAvatar.setImageResource(R.drawable.ic_person)
        holder.binding.btnChat.setOnClickListener {
            onItemClickChat(item)
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
        // 내 friends 중 친구(status 1)인 문서마다 users에서 친구 정보 찾기
        val uid = auth.currentUser?.uid ?: return
        val colref: CollectionReference = db.collection("users").document(uid).collection("friends")

        val query: Query = colref.whereEqualTo("status", 1)
        val task = query.get()
        task.addOnSuccessListener {
            list.clear()
            notifyDataSetChanged()
            for (document in it) {
                // 문서 id = 친구 uid
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

}