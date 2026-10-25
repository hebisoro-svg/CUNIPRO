package com.example.cunipro.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.databinding.ItemMortalityBinding
import com.example.cunipro.util.BreedingDateCalculator

class MortalityAdapter : ListAdapter<HealthLogEntity, MortalityAdapter.MortalityViewHolder>(MortalityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MortalityViewHolder {
        val binding = ItemMortalityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MortalityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MortalityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MortalityViewHolder(private val binding: ItemMortalityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: HealthLogEntity) {
            val titleStr = log.animalTag ?: "Lapin"
            binding.tvMortalityTitle.text = titleStr

            val dateStr = BreedingDateCalculator.formatDateDisplay(log.treatmentDate)
            val detailStr = if (log.deathCount > 1) {
                "${log.deathCount} lapereaux · $dateStr"
            } else {
                "$dateStr"
            }
            binding.tvMortalitySubtitle.text = detailStr

            val cause = log.deathCause ?: "Inconnue"
            binding.tvMortalityCauseBadge.text = cause

            when (cause.lowercase()) {
                "mort-né", "mort-ne", "mort ne" -> {
                    binding.tvMortalityCauseBadge.setBackgroundColor(Color.parseColor("#FFF3E0"))
                    binding.tvMortalityCauseBadge.setTextColor(Color.parseColor("#E65100"))
                }
                "chaleur" -> {
                    binding.tvMortalityCauseBadge.setBackgroundColor(Color.parseColor("#FFF8E1"))
                    binding.tvMortalityCauseBadge.setTextColor(Color.parseColor("#F57F17"))
                }
                "accident" -> {
                    binding.tvMortalityCauseBadge.setBackgroundColor(Color.parseColor("#E0F7FA"))
                    binding.tvMortalityCauseBadge.setTextColor(Color.parseColor("#00838F"))
                }
                else -> {
                    binding.tvMortalityCauseBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
                    binding.tvMortalityCauseBadge.setTextColor(Color.parseColor("#C62828"))
                }
            }
        }
    }

    class MortalityDiffCallback : DiffUtil.ItemCallback<HealthLogEntity>() {
        override fun areItemsTheSame(oldItem: HealthLogEntity, newItem: HealthLogEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HealthLogEntity, newItem: HealthLogEntity): Boolean =
            oldItem == newItem
    }
}
