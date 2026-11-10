package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ScheduleListItemBinding
import android.text.format.DateFormat


class itemViewHolder(
    val binding: ScheduleListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class listAdapter(
    private val list: List<GroupData>,
    private val onItemClick: (GroupData) -> Unit
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
        holder.binding.tvInfo.text = "${DateFormat.format("MM.dd (E) HH:mm", item.dateTime)} | ${item.place}"
        holder.binding.tvStatus.text = when(item.status) {
                GroupData.STATUS_RECRUITING -> "모집 중"
                GroupData.STATUS_CONFIRMED -> "확정"
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
