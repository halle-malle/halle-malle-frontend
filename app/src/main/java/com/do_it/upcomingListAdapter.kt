package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ScheduleListItemBinding
import com.do_it.databinding.UpcomingScheduleItemBinding

class upcomingItemViewHolder(
    val binding: UpcomingScheduleItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class upcomingListAdapter (
    private val list: List<cardData>,
    private val onItemClickChat: (cardData) -> Unit,
    private val onItemClickDetail: (cardData) -> Unit

) : RecyclerView.Adapter<upcomingItemViewHolder>() {

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

        holder.binding.tvPlace.text = "${item.date}|${item.place}"
        holder.binding.tvTitle.text = item.title
        holder.binding.tvNotice.text = "멤버 ${item.memberCount}/${item.maxMember} 공지:${item.notice}"
        holder.binding.btnChat.text = "그룹 채팅·새 메시지 ${item.newMessage}"
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


}