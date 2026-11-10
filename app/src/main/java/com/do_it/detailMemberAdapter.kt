package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ActivityGroupDetailBinding
import com.do_it.databinding.GroupDetailMemberListItemBinding
import com.do_it.databinding.UpcomingScheduleItemBinding

class detailMemberViewHolder(
    val binding:GroupDetailMemberListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class detailMemberAdapter (
    private val list: List<memberData>
): RecyclerView.Adapter<detailMemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): detailMemberViewHolder {
        val binding = GroupDetailMemberListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return detailMemberViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: detailMemberViewHolder,
        position: Int) {

        val item = list[position]

        holder.binding.tvName.text = item.name
        holder.binding.tvIntro.text = item.intro
        holder.binding.tvType.text = when(item.status) {
            memberData.STATUS_WAITING -> "대기중"
            memberData.STATUS_JOINED  -> when(item.role) {
                memberData.ROLE_HOST -> "방장"
                memberData.ROLE_MEMBER -> "멤버"
                else -> "멤버"
            }
            else -> "오류"
        }




    }

    override fun getItemCount(): Int {
        return list.size
    }

}