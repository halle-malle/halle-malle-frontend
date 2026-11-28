package com.do_it

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.ActivityGroupDetailBinding
import com.do_it.databinding.GroupDetailMemberListItemBinding
import com.do_it.databinding.UpcomingScheduleItemBinding

class detailMemberViewHolder(
    val binding:GroupDetailMemberListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class detailMemberAdapter (
    private val list: List<memberData>,
    private val onItemClickAddFriend: (memberData) -> Unit,
    private val onItemLongClick: (memberData) -> Unit // 항목 길게 클릭시 호출
): RecyclerView.Adapter<detailMemberViewHolder>() {

    private var friendIds = emptySet<String>()

    fun updateFriends(ids: Set<String>) {
        if (friendIds == ids) return
        friendIds = ids
        notifyDataSetChanged()
    }

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

        // 멤버 프로필 사진
        holder.binding.imageView.setImageResource(R.drawable.ic_person)
        db.collection("users").document(item.userId).get().addOnSuccessListener {
            val user = it.toObject(userData::class.java)
            if (user != null) {
                // 이미지가 있으면 프로필 사진 표시
                if(user.profileImageUrl.isNotBlank()) {
                    val rqMngr = Glide.with(holder.binding.root)
                    val rqBuilder = rqMngr.load(user.profileImageUrl)
                    rqBuilder.placeholder(R.drawable.ic_person)
                    rqBuilder.error(R.drawable.ic_person)
                    rqBuilder.override(300, 300)
                    rqBuilder.into(holder.binding.imageView)
                }
            }
        }
        if (item.userId == auth.currentUser?.uid || item.userId in friendIds) {
            holder.binding.btnAddFriend.visibility = View.GONE
            holder.binding.btnAddFriend.setOnClickListener(null)
        } else {
            holder.binding.btnAddFriend.visibility = View.VISIBLE
            holder.binding.btnAddFriend.setOnClickListener {
                onItemClickAddFriend(item)
            }
        }
        holder.binding.root.setOnLongClickListener {
            onItemLongClick(item)
            return@setOnLongClickListener true
        }



    }

    override fun getItemCount(): Int {
        return list.size
    }

}