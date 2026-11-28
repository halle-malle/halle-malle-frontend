package com.do_it

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FriendListItemBinding
import com.do_it.databinding.UpcomingScheduleItemBinding
import com.google.android.play.integrity.internal.q
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query

class friendItemViewHolder(
    val binding: FriendListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class friendListAdapter (
    private val onItemClickChat: (userData) -> Unit,
    private val onItemClickDetail: (userData) -> Unit,
    private val onItemLongClick: (userData) -> Unit,
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
        holder.binding.root.setOnLongClickListener {
            onItemLongClick(item)
            return@setOnLongClickListener true
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
                db.collection( "users").document(document.id).get().addOnSuccessListener {
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

    fun deleteFriend(item: userData) {
        val uid = auth.currentUser?.uid ?: return

        // firestore에서 친구 삭제 : 내 friends에서 친구, 친구 friends에서 나
        val colref : CollectionReference = db.collection("users").document(uid).collection("friends")
        val docref : DocumentReference = colref.document(item.id)
        val task = docref.delete()
        task.addOnSuccessListener {
            db.collection("users").document(item.id).collection("friends").document(uid).delete()
            toast("친구 삭제 완료")
            list.remove(item)
            notifyDataSetChanged()
        }
        task.addOnFailureListener { error ->
            toast("친구 삭제에 실패했습니다: ${error.localizedMessage}")
        }
    }

    fun reportFriend(item: userData) {
        // firestore에서 신고 횟수 읽어서 1 올리기 : 문서 id = 신고당한 사람 uid
        val colref : CollectionReference = db.collection("reports")
        val docref : DocumentReference = colref.document(item.id)
        val task = docref.get()
        task.addOnSuccessListener {
            val report = it.toObject(reportData::class.java)
            var count = 0
            if (report != null)   // 처음 신고당하는 경우는 문서가 없어서 null
                count = report.count

            val task2 = docref.set(reportData(
                userId = item.id,
                count = count + 1,
                reportedAt = System.currentTimeMillis()
            ))
            task2.addOnSuccessListener {
                toast("신고가 접수되었습니다.")
            }
            task2.addOnFailureListener { error ->
                toast("신고에 실패했습니다: ${error.localizedMessage}")
            }
        }
        task.addOnFailureListener { error ->
            toast("신고에 실패했습니다: ${error.localizedMessage}")
        }
    }
}
