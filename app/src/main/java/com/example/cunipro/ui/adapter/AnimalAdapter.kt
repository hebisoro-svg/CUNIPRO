package com.example.cunipro.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.databinding.ItemAnimalBinding
import com.example.cunipro.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnimalAdapter(
    private val onItemClick: (AnimalEntity) -> Unit
) : ListAdapter<AnimalEntity, AnimalAdapter.AnimalViewHolder>(AnimalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val binding = ItemAnimalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AnimalViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(animal: AnimalEntity) {
            val context = binding.root.context
            val isFr = LanguageManager.isFrench(context)

            // Tag Number
            binding.tvTagNumber.text = animal.tagNumber

            // Sex & Breed
            val sexDisplay = when (animal.sex.uppercase(Locale.getDefault())) {
                "F", "FEMELLE" -> if (isFr) "Femelle" else "Female"
                "M", "MÂLE", "MALE" -> if (isFr) "Mâle" else "Male"
                else -> animal.sex
            }
            val breedDisplay = animal.breed.takeIf { !it.isNullOrBlank() } ?: "Local"
            binding.tvSexAndBreed.text = "$sexDisplay · $breedDisplay"

            // Age & Weight
            val ageStr = calculateAgeMonths(animal.birthDate, isFr)
            val weightStr = if (animal.weightGrams != null && animal.weightGrams > 0) {
                val kg = animal.weightGrams / 1000.0
                String.format(Locale.getDefault(), "%.1f kg", kg)
            } else {
                "- kg"
            }
            binding.tvAgeAndWeight.text = "$ageStr · $weightStr"

            // Category Badge & Background
            val categoryUpper = animal.category.uppercase(Locale.getDefault())
            val (categoryText, bgDrawable, textColorHex) = when (categoryUpper) {
                "ENGRAISSEMENT" -> Triple(if (isFr) "Engraissement" else "Fattening", R.drawable.bg_badge_engraissement, "#E65100")
                "SEVRE", "SÉVRÉS" -> Triple(if (isFr) "Sevré" else "Weaned", R.drawable.bg_badge_sevre, "#0277BD")
                "VENDU" -> Triple(if (isFr) "Vendu" else "Sold", R.drawable.bg_badge_vendu, "#616161")
                "DECEDE", "DÉCÉDÉ" -> Triple(if (isFr) "Décédé" else "Deceased", R.drawable.bg_badge_decede, "#C62828")
                "PRIMIPARE" -> Triple(if (isFr) "Primipare" else "Primiparous", R.drawable.bg_badge_reproducteur, "#1B5E20")
                "MULTIPARE" -> Triple(if (isFr) "Multipare" else "Multiparous", R.drawable.bg_badge_reproducteur, "#1B5E20")
                else -> Triple(if (isFr) "Reproducteur" else "Breeder", R.drawable.bg_badge_reproducteur, "#1B5E20")
            }

            binding.tvCategoryBadge.text = categoryText
            binding.tvCategoryBadge.setBackgroundResource(bgDrawable)
            binding.tvCategoryBadge.setTextColor(Color.parseColor(textColorHex))

            // Card click listener
            binding.cardAnimal.setOnClickListener {
                onItemClick(animal)
            }
        }

        private fun calculateAgeMonths(birthDateStr: String?, isFr: Boolean): String {
            if (birthDateStr.isNullOrBlank()) return if (isFr) "Âge inconnu" else "Unknown age"
            return try {
                val inputFormats = arrayOf(
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
                )

                var date: java.util.Date? = null
                for (format in inputFormats) {
                    try {
                        date = format.parse(birthDateStr)
                        if (date != null) break
                    } catch (_: Exception) {}
                }

                if (date == null) return birthDateStr

                val birthCal = Calendar.getInstance().apply { time = date }
                val nowCal = Calendar.getInstance()

                var months = (nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12 +
                        (nowCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH))

                if (nowCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) {
                    months--
                }

                when {
                    months <= 0 -> if (isFr) "Moins d'1 mois" else "Under 1 month"
                    months < 12 -> if (isFr) "$months mois" else "$months months"
                    else -> {
                        val years = months / 12
                        val remMonths = months % 12
                        if (remMonths > 0) {
                            if (isFr) "$years an(s) $remMonths m" else "$years yr $remMonths mo"
                        } else {
                            if (isFr) "$years an(s)" else "$years yr"
                        }
                    }
                }
            } catch (_: Exception) {
                birthDateStr
            }
        }
    }

    class AnimalDiffCallback : DiffUtil.ItemCallback<AnimalEntity>() {
        override fun areItemsTheSame(oldItem: AnimalEntity, newItem: AnimalEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AnimalEntity, newItem: AnimalEntity): Boolean =
            oldItem == newItem
    }
}
