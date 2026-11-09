package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.do_it.databinding.GroupChatMineListItemBinding
import com.do_it.databinding.GroupChatOtherListItemBinding
import com.do_it.databinding.GroupDetailMemberListItemBinding

class ChatMineViewHolder(val binding: GroupChatMineListItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class ChatOtherViewHolder(val binding: GroupChatOtherListItemBinding) :
    RecyclerView.ViewHolder(binding.root)

class GroupChatAdapter(
    private val chatList: List<chatData>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //상대방 채팅인지 나의 채팅인지 확인 position의 아이템은 어떤 모양의 뷰로 보여줘야되는지
    override fun getItemViewType(position: Int): Int {
        return chatList[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == chatData.TYPE_MINE) {
            ChatMineViewHolder(GroupChatMineListItemBinding.inflate(inflater, parent, false))
        } else {
            ChatOtherViewHolder(GroupChatOtherListItemBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = chatList[position]   // 데이터는 하나

        when (holder) {
            is ChatMineViewHolder -> {
                holder.binding.tvMyContent.text = item.content
                holder.binding.tvTime.text = item.time
            }
            is ChatOtherViewHolder -> {
                holder.binding.tvName.text = item.name
                holder.binding.tvOtherContent.text = item.content
                holder.binding.tvTime.text = item.time
            }
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }
}


