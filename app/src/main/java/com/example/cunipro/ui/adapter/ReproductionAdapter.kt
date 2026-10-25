package com.example.cunipro.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.ReproductionEntity
import com.example.cunipro.databinding.ItemReproductionBinding
import com.example.cunipro.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReproductionAdapter(
    private val onKindlingClick: (ReproductionEntity) -> Unit,
    private val onWeaningClick: (ReproductionEntity) -> Unit
) : ListAdapter<ReproductionEntity, ReproductionAdapter.ReproductionViewHolder>(ReproductionDiffCallback()) {

    companion object {
        const val TAB_GESTATIONS = 0
        const val TAB_MISES_BAS = 1
        const val TAB_SEVRAGES = 2
    }

    var currentTab: Int = TAB_GESTATIONS
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReproductionViewHolder {
        val binding = ItemReproductionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReproductionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReproductionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReproductionViewHolder(private val binding: ItemReproductionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(repro: ReproductionEntity) {
            val context = binding.root.context
            val isFr = LanguageManager.isFrench(context)
            val unknownMale = if (isFr) "Inconnu" else "Unknown"
            binding.tvCoupleTags.text = "${repro.femaleTag} × ${repro.maleTag ?: unknownMale}"

            when (currentTab) {
                TAB_GESTATIONS -> bindGestationView(repro, isFr)
                TAB_MISES_BAS -> bindKindlingView(repro, isFr)
                TAB_SEVRAGES -> bindWeaningView(repro, isFr)
            }
        }

        private fun bindGestationView(repro: ReproductionEntity, isFr: Boolean) {
            binding.layoutProgressSection.visibility = View.VISIBLE
            binding.layoutTimelineSteps.visibility = View.VISIBLE
            binding.cardKindlingStatsRow.visibility = View.GONE
            binding.tvFooterDateInfo.visibility = View.GONE
            binding.btnActionSevrage.visibility = View.GONE

            val matingLabel = if (isFr) "Saillie du" else "Mating on"
            binding.tvReproSubtitle.text = "$matingLabel ${formatDisplayDate(repro.matingDate)}"

            val context = binding.root.context
            val gestationDays = com.example.cunipro.util.BreedingDateCalculator.getGestationDays(context)
            val palpationDays = com.example.cunipro.util.BreedingDateCalculator.getPalpationDays(context)

            val daysElapsed = calculateDaysSince(repro.matingDate)
            val progress = ((daysElapsed / gestationDays.toDouble()) * 100).coerceIn(0.0, 100.0).toInt()
            binding.progressBarGestation.progress = progress
            val dayLabel = if (isFr) "Jour $daysElapsed sur $gestationDays" else "Day $daysElapsed of $gestationDays"
            binding.tvProgressDaysText.text = dayLabel

            val daysUntilKindling = (gestationDays - daysElapsed).coerceAtLeast(0)
            binding.tvTopRightBadge.text = if (daysUntilKindling == 0) {
                if (isFr) "Mise bas aujourd'hui" else "Kindling today"
            } else {
                if (isFr) "Mise bas dans $daysUntilKindling jours" else "Kindling in $daysUntilKindling days"
            }
            binding.tvTopRightBadge.setBackgroundResource(R.drawable.bg_badge_engraissement)
            binding.tvTopRightBadge.setTextColor(android.graphics.Color.parseColor("#E65100"))

            binding.tvTimelineDate1.text = formatShortDate(repro.palpationDate ?: repro.matingDate)
            binding.tvTimelineDate2.text = formatShortDate(repro.expectedKindlingDate ?: repro.matingDate)
            binding.tvTimelineDate3.text = formatShortDate(repro.expectedWeaningDate)

            val isPalpationDone = daysElapsed >= palpationDays
            val isKindlingDone = daysElapsed >= gestationDays || repro.actualKindlingDate != null

            val palpationText = if (isFr) "Palpation" else "Palpation"
            val kindlingText = if (isFr) "Mise bas" else "Kindling"
            val weaningText = if (isFr) "Sevrage" else "Weaning"

            if (isPalpationDone) {
                binding.tvTimelineStep1.text = "✔ $palpationText"
                binding.tvTimelineStep1.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            } else {
                binding.tvTimelineStep1.text = "⭕ $palpationText"
                binding.tvTimelineStep1.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            }

            if (isKindlingDone) {
                binding.tvTimelineStep2.text = "✔ $kindlingText"
                binding.tvTimelineStep2.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            } else {
                binding.tvTimelineStep2.text = "⭕ $kindlingText"
                binding.tvTimelineStep2.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            }

            binding.tvTimelineStep3.text = "⚪ $weaningText"
            binding.tvTimelineStep3.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
        }

        private fun bindKindlingView(repro: ReproductionEntity, isFr: Boolean) {
            binding.layoutProgressSection.visibility = View.GONE
            binding.layoutTimelineSteps.visibility = View.GONE

            if (repro.actualKindlingDate.isNullOrBlank()) {
                binding.cardKindlingStatsRow.visibility = View.GONE
                binding.tvFooterDateInfo.visibility = View.VISIBLE
                binding.btnActionSevrage.visibility = View.VISIBLE
                binding.btnActionSevrage.text = if (isFr) "Enregistrer la mise bas" else "Record kindling"

                val daysElapsed = calculateDaysSince(repro.matingDate)
                val daysUntil = 31 - daysElapsed
                val badgeText = when {
                    daysUntil > 1 -> if (isFr) "Dans $daysUntil jours" else "In $daysUntil days"
                    daysUntil == 1 -> if (isFr) "Dans 1 jour" else "In 1 day"
                    daysUntil == 0 -> if (isFr) "Aujourd'hui" else "Today"
                    else -> if (isFr) "En retard (${-daysUntil} j)" else "Overdue (${-daysUntil} d)"
                }
                binding.tvTopRightBadge.text = badgeText
                binding.tvTopRightBadge.setBackgroundResource(R.drawable.bg_badge_engraissement)
                binding.tvTopRightBadge.setTextColor(android.graphics.Color.parseColor("#E65100"))

                val expectedLabel = if (isFr) "Mise bas prévue le" else "Kindling expected on"
                val matingLabel = if (isFr) "Saillie du" else "Mating on"
                binding.tvReproSubtitle.text = "$expectedLabel ${formatDisplayDate(repro.expectedKindlingDate ?: repro.matingDate)}"
                binding.tvFooterDateInfo.text = "📅 $matingLabel ${formatDisplayDate(repro.matingDate)}"

                binding.btnActionSevrage.setOnClickListener {
                    onKindlingClick(repro)
                }
            } else {
                binding.cardKindlingStatsRow.visibility = View.VISIBLE
                binding.tvFooterDateInfo.visibility = View.VISIBLE
                binding.btnActionSevrage.visibility = View.GONE

                val daysAgo = calculateDaysSince(repro.actualKindlingDate)
                binding.tvTopRightBadge.text = if (daysAgo == 0) {
                    if (isFr) "Aujourd'hui" else "Today"
                } else {
                    if (isFr) "Il y a $daysAgo jours" else "$daysAgo days ago"
                }
                binding.tvTopRightBadge.setBackgroundResource(R.drawable.bg_badge_vendu)
                binding.tvTopRightBadge.setTextColor(android.graphics.Color.parseColor("#616161"))

                val kindlingLabel = if (isFr) "Mise bas du" else "Kindling on"
                binding.tvReproSubtitle.text = "$kindlingLabel ${formatDisplayDate(repro.actualKindlingDate)}"

                val total = repro.bornAliveCount + repro.bornDeadCount
                binding.tvTotalBorn.text = total.toString()
                binding.tvAliveBorn.text = repro.bornAliveCount.toString()
                binding.tvDeadBorn.text = repro.bornDeadCount.toString()

                val expectedWeaningLabel = if (isFr) "Sevrage prévu le" else "Expected weaning on"
                binding.tvFooterDateInfo.text = "📅 $expectedWeaningLabel ${formatDisplayDate(repro.expectedWeaningDate)}"
            }
        }

        private fun bindWeaningView(repro: ReproductionEntity, isFr: Boolean) {
            binding.layoutProgressSection.visibility = View.VISIBLE
            binding.layoutTimelineSteps.visibility = View.GONE
            binding.cardKindlingStatsRow.visibility = View.GONE
            binding.tvFooterDateInfo.visibility = View.VISIBLE
            binding.btnActionSevrage.visibility = View.VISIBLE
            binding.btnActionSevrage.text = if (isFr) "Sevrages" else "Wean"

            val kindlingDate = repro.actualKindlingDate ?: repro.matingDate
            val daysElapsed = calculateDaysSince(kindlingDate)
            val progress = ((daysElapsed / 35.0) * 100).coerceIn(0.0, 100.0).toInt()
            binding.progressBarGestation.progress = progress
            val dayLabel = if (isFr) "Jour $daysElapsed sur 35" else "Day $daysElapsed of 35"
            binding.tvProgressDaysText.text = dayLabel

            val daysLeft = (35 - daysElapsed).coerceAtLeast(0)
            binding.tvTopRightBadge.text = if (daysLeft == 0) {
                if (isFr) "Prévu aujourd'hui" else "Due today"
            } else {
                if (isFr) "Dans $daysLeft jours" else "In $daysLeft days"
            }
            binding.tvTopRightBadge.setBackgroundResource(R.drawable.bg_badge_sevre)
            binding.tvTopRightBadge.setTextColor(android.graphics.Color.parseColor("#0277BD"))

            val kitsLabel = if (isFr) "lapereaux" else "kits"
            val daysLbl = if (isFr) "jours" else "days"
            binding.tvReproSubtitle.text = "${repro.bornAliveCount} $kitsLabel · $daysElapsed $daysLbl"
            val expectedWeaningLabel = if (isFr) "Sevrage prévu le" else "Expected weaning on"
            binding.tvFooterDateInfo.text = "📅 $expectedWeaningLabel ${formatDisplayDate(repro.expectedWeaningDate)}"

            binding.btnActionSevrage.setOnClickListener {
                onWeaningClick(repro)
            }
        }

        private fun calculateDaysSince(isoDateStr: String?): Int {
            if (isoDateStr.isNullOrBlank()) return 0
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val date = sdf.parse(isoDateStr) ?: return 0
                val diff = Date().time - date.time
                (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            } catch (_: Exception) {
                0
            }
        }

        private fun formatDisplayDate(isoDateStr: String?): String {
            if (isoDateStr.isNullOrBlank()) return "-"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.FRANCE)
                val date = inputFormat.parse(isoDateStr)
                if (date != null) outputFormat.format(date) else isoDateStr
            } catch (_: Exception) {
                isoDateStr
            }
        }

        private fun formatShortDate(isoDateStr: String?): String {
            if (isoDateStr.isNullOrBlank()) return "-"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                val outputFormat = SimpleDateFormat("d MMM", Locale.FRANCE)
                val date = inputFormat.parse(isoDateStr)
                if (date != null) outputFormat.format(date) else isoDateStr
            } catch (_: Exception) {
                isoDateStr
            }
        }
    }

    class ReproductionDiffCallback : DiffUtil.ItemCallback<ReproductionEntity>() {
        override fun areItemsTheSame(oldItem: ReproductionEntity, newItem: ReproductionEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ReproductionEntity, newItem: ReproductionEntity): Boolean =
            oldItem == newItem
    }
}
