package com.example.cunipro.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.databinding.ItemHealthBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager

class HealthAdapter(
    private val onItemClick: ((HealthLogEntity) -> Unit)? = null
) : ListAdapter<HealthLogEntity, HealthAdapter.HealthViewHolder>(HealthDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthViewHolder {
        val binding = ItemHealthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HealthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HealthViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HealthViewHolder(private val binding: ItemHealthBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: HealthLogEntity) {
            val context = binding.root.context
            val isFr = LanguageManager.isFrench(context)

            val titleText = if (log.diseaseName.isNotBlank()) log.diseaseName else log.treatmentName
            binding.tvHealthDiseaseTitle.text = titleText

            val defaultTarget = if (isFr) "Élevage complet" else "Entire herd"
            val target = log.animalTag ?: defaultTarget
            val formattedDate = BreedingDateCalculator.formatDateDisplay(log.treatmentDate)
            binding.tvHealthTargetAndDate.text = "$target · $formattedDate"

            if (log.status.equals("EN_COURS", ignoreCase = true)) {
                binding.tvHealthStatusBadge.text = if (isFr) "En cours" else "Ongoing"
                binding.tvHealthStatusBadge.setBackgroundResource(R.drawable.bg_pill_green_container)
                binding.tvHealthStatusBadge.setTextColor(Color.parseColor("#1B5E20"))
                binding.cardIconHealth.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
                binding.tvIconEmoji.text = "💊"
            } else {
                binding.tvHealthStatusBadge.text = if (isFr) "Terminé" else "Completed"
                binding.tvHealthStatusBadge.setBackgroundResource(R.drawable.bg_pill_green_light)
                binding.tvHealthStatusBadge.setTextColor(Color.parseColor("#2E7D32"))
                binding.cardIconHealth.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                binding.tvIconEmoji.text = "🩺"
            }

            val treatmentLabel = if (isFr) "Traitement" else "Treatment"
            val dosageLabel = if (isFr) "Dosage" else "Dosage"
            val treatment = if (log.treatmentName.isNotBlank()) log.treatmentName else "-"
            val dosage = log.dosage ?: "-"
            binding.tvHealthTreatmentDetails.text = "$treatmentLabel : $treatment · $dosageLabel : $dosage"

            binding.root.setOnClickListener {
                onItemClick?.invoke(log)
            }
        }
    }

    class HealthDiffCallback : DiffUtil.ItemCallback<HealthLogEntity>() {
        override fun areItemsTheSame(oldItem: HealthLogEntity, newItem: HealthLogEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: HealthLogEntity, newItem: HealthLogEntity): Boolean =
            oldItem == newItem
    }
}
