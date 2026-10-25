package com.example.cunipro.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.FeedStockEntity
import com.example.cunipro.databinding.ItemFeedStockBinding
import com.example.cunipro.util.LanguageManager
import java.util.Locale

class FeedStockAdapter(
    private val onItemClick: ((FeedStockEntity) -> Unit)? = null
) : ListAdapter<FeedStockEntity, FeedStockAdapter.FeedStockViewHolder>(FeedStockDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedStockViewHolder {
        val binding = ItemFeedStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedStockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedStockViewHolder(private val binding: ItemFeedStockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: FeedStockEntity) {
            val context = binding.root.context
            val isFr = LanguageManager.isFrench(context)

            binding.tvFeedName.text = stock.feedName

            val thresholdLabel = if (isFr) "Seuil d'alerte : %.0f kg" else "Alert threshold: %.0f kg"
            binding.tvAlertThresholdInfo.text = String.format(Locale.getDefault(), thresholdLabel, stock.minThresholdKg)

            // Quantity & Low Stock Badge Check
            val isLowStock = stock.quantityKg <= stock.minThresholdKg
            if (isLowStock) {
                binding.tvStockStatusBadge.text = if (isFr) "Stock faible" else "Low stock"
                binding.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_engraissement)
                binding.tvStockStatusBadge.setTextColor(Color.parseColor("#E65100"))
            } else {
                binding.tvStockStatusBadge.text = if (isFr) "Suffisant" else "Sufficient"
                binding.tvStockStatusBadge.setBackgroundResource(R.drawable.bg_badge_reproducteur)
                binding.tvStockStatusBadge.setTextColor(Color.parseColor("#1B5E20"))
            }

            binding.tvQuantityKg.text = String.format(Locale.FRANCE, "%.0f kg", stock.quantityKg)

            // Icon Background
            if (stock.feedName.equals("Granulés", ignoreCase = true)) {
                binding.cardFeedIcon.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
                binding.ivFeedIcon.setColorFilter(Color.parseColor("#F57C00"))
            } else {
                binding.cardFeedIcon.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                binding.ivFeedIcon.setColorFilter(Color.parseColor("#2E7D32"))
            }

            binding.cardFeedStock.setOnClickListener {
                onItemClick?.invoke(stock)
            }
        }
    }

    class FeedStockDiffCallback : DiffUtil.ItemCallback<FeedStockEntity>() {
        override fun areItemsTheSame(oldItem: FeedStockEntity, newItem: FeedStockEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FeedStockEntity, newItem: FeedStockEntity): Boolean =
            oldItem == newItem
    }
}
