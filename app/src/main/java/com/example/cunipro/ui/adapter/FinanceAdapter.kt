package com.example.cunipro.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.data.local.entity.FinanceLogEntity
import com.example.cunipro.databinding.ItemFinanceBinding
import com.example.cunipro.util.BreedingDateCalculator

class FinanceAdapter : ListAdapter<FinanceLogEntity, FinanceAdapter.FinanceViewHolder>(FinanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FinanceViewHolder {
        val binding = ItemFinanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FinanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FinanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FinanceViewHolder(private val binding: ItemFinanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: FinanceLogEntity) {
            binding.tvFinanceDescription.text = log.description
            val dateStr = BreedingDateCalculator.formatDateDisplay(log.transactionDate)
            binding.tvFinanceCategoryAndDate.text = "${log.category} | $dateStr"

            val prefix = if (log.type == "RECETTE") "+" else "-"
            binding.tvFinanceAmount.text = "$prefix ${log.amount} FCFA"
        }
    }

    class FinanceDiffCallback : DiffUtil.ItemCallback<FinanceLogEntity>() {
        override fun areItemsTheSame(oldItem: FinanceLogEntity, newItem: FinanceLogEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FinanceLogEntity, newItem: FinanceLogEntity): Boolean =
            oldItem == newItem
    }
}
