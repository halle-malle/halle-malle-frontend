package com.do_it

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.do_it.databinding.ListLayoutBinding

class CardAdapter(
    private val list: List<CardData>
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ListLayoutBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = list[position]

        holder.binding.tvTitle.text = item.title
        holder.binding.tvInfo.text = item.info
        holder.binding.tvStatus.text = item.status
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
