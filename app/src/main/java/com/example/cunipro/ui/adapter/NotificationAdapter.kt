package com.example.cunipro.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.NotificationEntity
import com.example.cunipro.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val onNotificationClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationEntity) {
            binding.tvNotificationTitle.text = item.title
            binding.tvNotificationMessage.text = item.message
            binding.tvNotificationTime.text = item.time

            // Unread dot
            binding.vUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

            // Type specific styling
            when (item.type.uppercase()) {
                "STOCK" -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#FFF8E1"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_sprout)
                }
                "MISE_BAS", "PALPATION" -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_heart)
                }
                "TASK", "IMPORTANT" -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_flag)
                }
                "SOIN" -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#E1F5FE"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_medical)
                }
                "SEVRAGE" -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_calendar)
                }
                else -> {
                    binding.iconContainer.setBackgroundColor(Color.parseColor("#E8F5E9"))
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_task)
                }
            }

            binding.root.setOnClickListener {
                onNotificationClick(item)
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean =
            oldItem == newItem
    }
}
