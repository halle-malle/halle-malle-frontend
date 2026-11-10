package com.do_it

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.UpcomingScheduleItemBinding
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query

class upcomingItemViewHolder(
    val binding: UpcomingScheduleItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class upcomingListAdapter (
    private val onItemClickChat: (GroupData) -> Unit,
    private val onItemClickDetail: (GroupData) -> Unit,
    private val toast: (String) -> Unit // 어답터 클래스 안에서 토스트 창을 띄워야 할 때 호출하는 함수

) : RecyclerView.Adapter<upcomingItemViewHolder>() {

    private var list: MutableList<GroupData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): upcomingItemViewHolder {
        val binding = UpcomingScheduleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return upcomingItemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: upcomingItemViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvPlace.text = "${DateFormat.format("MM.dd (E) HH:mm", item.dateTime)}|${item.place}"
        holder.binding.tvTitle.text = item.title
        holder.binding.tvNotice.text = "멤버 ${item.memberCount}/${item.maxMember} 공지:${item.notice}"
        holder.binding.btnChat.text = "그룹 채팅·새 메시지"
        holder.binding.btnDetail.text = "모임 상세"
        holder.binding.btnChat.setOnClickListener {
            onItemClickChat(item)
        }
        holder.binding.btnDetail.setOnClickListener{
            onItemClickDetail(item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
    fun loadItems() {
        // firestore에서 status가 0, 1인것만 가져오는 코드
        val colref : CollectionReference = db.collection("groups")

        val query: Query = colref.whereIn("status", listOf(0, 1))
        val task = query.get()
        task.addOnSuccessListener {
            val dbList = mutableListOf<GroupData>()
            for(document in it) {
                val item = document.toObject(GroupData::class.java)
                dbList.add(item)
            }
            list = dbList
            notifyDataSetChanged()
        }
        task.addOnFailureListener {
            toast("읽기 실패")
        }
    }

}