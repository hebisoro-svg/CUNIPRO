package com.example.cunipro.ui.salesfinance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.FinanceLogEntity
import com.example.cunipro.databinding.ItemFinanceExpenseBinding
import com.example.cunipro.databinding.ItemFinanceSaleBinding
import com.example.cunipro.util.BreedingDateCalculator
import java.util.Locale

class SalesAdapter : ListAdapter<FinanceLogEntity, SalesAdapter.SaleViewHolder>(FinanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val binding = ItemFinanceSaleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SaleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SaleViewHolder(private val binding: ItemFinanceSaleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: FinanceLogEntity) {
            val emoji = when (log.category.uppercase(Locale.ROOT)) {
                "VENTE_LAPIN", "LAPIN", "LAPINS" -> "🐰"
                "VENTE_ALIMENT", "ALIMENT", "ALIMENTS" -> "🌾"
                "VENTE_MATERIEL", "MATERIEL" -> "🔧"
                else -> "🛍️"
            }
            binding.tvSaleTypeIcon.text = emoji

            val defaultTitle = when (log.category.uppercase(Locale.ROOT)) {
                "VENTE_LAPIN", "LAPIN", "LAPINS" -> "Vente de lapins"
                "VENTE_ALIMENT", "ALIMENT", "ALIMENTS" -> "Vente d'aliments"
                "VENTE_MATERIEL", "MATERIEL" -> "Vente de matériel"
                else -> log.description.ifBlank { "Vente" }
            }
            binding.tvSaleTitle.text = defaultTitle

            val dateStr = BreedingDateCalculator.formatDateDisplay(log.transactionDate)
            val clientStr = if (!log.clientName.isNullOrBlank()) log.clientName else "Client non spécifié"
            binding.tvSaleClientAndDate.text = "$clientStr · $dateStr"

            val details = log.itemDetails ?: log.description
            binding.tvSaleDetails.text = details.ifBlank { "—" }

            val formattedAmount = String.format(Locale.getDefault(), "+%,.0f FCFA", log.amount)
            binding.tvSaleAmount.text = formattedAmount
        }
    }
}

class ExpenseAdapter : ListAdapter<FinanceLogEntity, ExpenseAdapter.ExpenseViewHolder>(FinanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemFinanceExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExpenseViewHolder(private val binding: ItemFinanceExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: FinanceLogEntity) {
            val context = binding.root.context
            val isFeed = log.category.contains("ALIMENT", ignoreCase = true) || log.category.contains("ALIMENTATION", ignoreCase = true)
            val isHealth = log.category.contains("SOIN", ignoreCase = true) || log.category.contains("SANTÉ", ignoreCase = true) || log.category.contains("SANTE", ignoreCase = true)

            val emoji = when {
                isFeed -> "🌾"
                isHealth -> "💊"
                else -> "📄"
            }
            binding.tvExpenseTypeIcon.text = emoji

            val bgRes = when {
                isFeed -> R.drawable.bg_pill_green_light
                isHealth -> R.drawable.bg_pill_white
                else -> R.drawable.bg_pill_white
            }
            binding.containerExpenseAvatar.setBackgroundResource(bgRes)

            binding.tvExpenseTitle.text = log.description.ifBlank { "Dépense" }

            val dateStr = BreedingDateCalculator.formatDateDisplay(log.transactionDate)
            val categoryLabel = when {
                isFeed -> "Alimentation"
                isHealth -> "Santé"
                else -> "Autres charges"
            }
            binding.tvExpenseCategoryAndDate.text = "$categoryLabel · $dateStr"

            val formattedAmount = String.format(Locale.getDefault(), "-%,.0f FCFA", log.amount)
            binding.tvExpenseAmount.text = formattedAmount
        }
    }
}

class FinanceDiffCallback : DiffUtil.ItemCallback<FinanceLogEntity>() {
    override fun areItemsTheSame(oldItem: FinanceLogEntity, newItem: FinanceLogEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FinanceLogEntity, newItem: FinanceLogEntity): Boolean {
        return oldItem == newItem
    }
}
