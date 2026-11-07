package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ScheduleListItemBinding


class itemViewHolder(
    val binding: ScheduleListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class listAdapter(
    private val list: List<cardData>,
    private val onItemClick: (cardData) -> Unit
) : RecyclerView.Adapter<itemViewHolder>() {



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): itemViewHolder {

        val binding = ScheduleListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return itemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: itemViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvTitle.text = item.title
        holder.binding.tvInfo.text = "${item.date} | ${item.place}"
        holder.binding.tvStatus.text = when(item.status) {
                cardData.STATUS_HOST -> "멤버 ${item.memberCount}/${item.maxMember}명"
                cardData.STATUS_JOINED -> "참여중"
                cardData.STATUS_WAITING -> "다가오는 일정"
                else -> "에러"
        }
        holder.itemView.setOnClickListener{
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
