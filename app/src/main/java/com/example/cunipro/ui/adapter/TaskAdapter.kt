package com.example.cunipro.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.data.local.entity.TaskEntity
import com.example.cunipro.databinding.ItemTaskBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager

class TaskAdapter(
    private val onTaskCheckedChange: (TaskEntity, Boolean) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: TaskEntity) {
            val context = binding.root.context
            val isFr = LanguageManager.isFrench(context)

            binding.tvTaskTitle.text = task.title

            // Strikethrough effect on title if completed
            if (task.isCompleted) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Flag icon for important tasks
            binding.ivFlagIcon.visibility = if (task.isFlagged || task.priority == "HAUTE") View.VISIBLE else View.GONE

            // Subtitle formatting: "Category · Date, Time · Repeat"
            val categoryPart = task.category ?: task.type
            val formattedDate = BreedingDateCalculator.formatDateDisplay(task.dueDate)
            val repeatPart = if (!task.repeatInfo.isNullOrBlank()) " · ${task.repeatInfo}" else ""
            binding.tvTaskSubtitle.text = "$categoryPart · $formattedDate, ${task.dueTime}$repeatPart"

            // Reminder formatting
            val reminderPrefix = if (isFr) "🔔 Rappel : " else "🔔 Reminder: "
            binding.tvTaskReminder.text = "$reminderPrefix${task.reminderInfo}"

            // Auto Badge
            binding.tvAutoBadge.visibility = if (task.isAuto) View.VISIBLE else View.GONE

            // Checkbox
            binding.cbTaskCompleted.setOnCheckedChangeListener(null)
            binding.cbTaskCompleted.isChecked = task.isCompleted

            binding.cbTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskCheckedChange(task, isChecked)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean =
            oldItem == newItem
    }
}
