package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ScheduleListItemBinding
import android.text.format.DateFormat
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query


class itemViewHolder(
    val binding: ScheduleListItemBinding
) : RecyclerView.ViewHolder(binding.root) {

}

class listAdapter(

    private val onItemClick: (GroupData) -> Unit,
    private val toast: (String) -> Unit // 어답터 클래스 안에서 토스트 창을 띄워야 할 때 호출하는 함수
) : RecyclerView.Adapter<itemViewHolder>() {

    private var list: List<GroupData> = listOf()


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

    fun loadItems() {
        // firestore에서 status가 0, 1인것만 가져오는 코드
        val colref : CollectionReference = db.collection("groups")

        val query: Query = colref.whereEqualTo("hostId", auth.currentUser?.uid).whereIn("status", listOf(0, 1))
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

    fun loadJoinedItems() {
        // firestore에서 status가 0, 1인것만 가져오는 코드
        val colref : CollectionReference = db.collection("groups")
        val uid = auth.currentUser?.uid ?: return
        val query: Query = colref.whereArrayContains("memberIds", uid).whereIn("status", listOf(0, 1))
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
