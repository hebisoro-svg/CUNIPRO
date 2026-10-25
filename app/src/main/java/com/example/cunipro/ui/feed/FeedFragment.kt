package com.example.cunipro.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.FeedDistributionEntity
import com.example.cunipro.data.local.entity.FeedStockEntity
import com.example.cunipro.databinding.FragmentFeedBinding
import com.example.cunipro.ui.adapter.FeedStockAdapter
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupTopActions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        FeedViewModelFactory(app.feedRepository)
    }

    private lateinit var feedAdapter: FeedStockAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopActions(
            binding.includeTopActions.btnTopFinance,
            binding.includeTopActions.btnTopNotifications,
            binding.includeTopActions.btnTopSettings,
            binding.includeTopActions.tvNotificationBadge
        )

        setupRecyclerView()
        setupTabToggle()
        observeData()

        binding.btnAddStock.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_addFeedStockFragment)
        }

        binding.btnRemoveStock.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_recordFeedConsumptionFragment)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("showConsumptionTab")
            ?.observe(viewLifecycleOwner) { show ->
                if (show == true) {
                    updateTabState(isStockTab = false)
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("showConsumptionTab")
                }
            }
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedStockAdapter()
        binding.rvFeedStock.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }
    }

    private fun setupTabToggle() {
        binding.btnTabStock.setOnClickListener {
            updateTabState(isStockTab = true)
        }

        binding.btnTabConsumption.setOnClickListener {
            updateTabState(isStockTab = false)
        }
    }

    private fun updateTabState(isStockTab: Boolean) {
        val greenPrimaryDark = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.cuni_text_primary)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        if (isStockTab) {
            binding.layoutStockSection.visibility = View.VISIBLE
            binding.layoutConsumptionSection.visibility = View.GONE

            binding.btnTabStock.apply {
                setBackgroundColor(greenPrimaryDark)
                setTextColor(white)
            }
            binding.btnTabConsumption.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textPrimary)
            }
        } else {
            binding.layoutStockSection.visibility = View.GONE
            binding.layoutConsumptionSection.visibility = View.VISIBLE

            binding.btnTabConsumption.apply {
                setBackgroundColor(greenPrimaryDark)
                setTextColor(white)
            }
            binding.btnTabStock.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textPrimary)
            }

            viewModel.allDistributions.value?.let { distributions ->
                updateConsumptionUI(distributions)
            }
        }
    }

    private fun observeData() {
        viewModel.feedStockList.observe(viewLifecycleOwner) { stocks ->
            if (_binding == null) return@observe
            feedAdapter.submitList(stocks)

            val totalTypes = stocks.size
            val lowStockItems = stocks.filter { it.quantityKg <= it.minThresholdKg }
            val lowStockCount = lowStockItems.size

            val isFr = LanguageManager.isFrench(requireContext())
            val typeStr = if (isFr) "aliments en stock" else "feed items in stock"
            val alertStr = if (isFr) "alerte de stock faible" else "low stock alert(s)"
            binding.tvFeedHeaderSubtitle.text = "$totalTypes $typeStr · $lowStockCount $alertStr"

            val lowStockItem = lowStockItems.firstOrNull()
            if (lowStockItem != null) {
                binding.cardLowStockWarning.visibility = View.VISIBLE
                updateLowStockWarningCard(lowStockItem, viewModel.allDistributions.value ?: emptyList())
            } else {
                binding.cardLowStockWarning.visibility = View.GONE
            }
        }

        viewModel.allDistributions.observe(viewLifecycleOwner) { distributions ->
            if (_binding == null) return@observe
            updateConsumptionUI(distributions)

            val stocks = viewModel.feedStockList.value ?: emptyList()
            val lowStockItem = stocks.find { it.quantityKg <= it.minThresholdKg }
            if (lowStockItem != null) {
                updateLowStockWarningCard(lowStockItem, distributions)
            }
        }
    }

    private fun updateLowStockWarningCard(lowStockItem: FeedStockEntity, distributions: List<FeedDistributionEntity>) {
        if (_binding == null) return

        val feedName = lowStockItem.feedName
        val remainingKg = lowStockItem.quantityKg
        val thresholdKg = lowStockItem.minThresholdKg
        val isFr = LanguageManager.isFrench(requireContext())

        val titlePrefix = if (isFr) "Stock faible : " else "Low stock: "
        binding.tvWarningTitle.text = "$titlePrefix${feedName.lowercase(Locale.getDefault())}"

        val feedDistributions = distributions.filter {
            val name = it.feedName.trim().lowercase(Locale.getDefault())
            val target = feedName.trim().lowercase(Locale.getDefault())
            name == target || name.contains(target) || target.contains(name)
        }

        val uniqueDaysCount = feedDistributions.map { it.distributionDate.trim().take(10) }.distinct().size
        val totalDistributedKg = feedDistributions.sumOf { it.quantityKg }

        val dailyRateKg = if (uniqueDaysCount > 0 && totalDistributedKg > 0) {
            totalDistributedKg / uniqueDaysCount
        } else {
            0.0
        }

        val forecastText = if (dailyRateKg > 0) {
            val remainingDays = remainingKg / dailyRateKg
            val formattedDays = if (isFr) {
                when {
                    remainingDays < 0.5 -> "moins d'un jour"
                    remainingDays < 1.5 -> "environ 1 jour"
                    else -> "environ ${Math.round(remainingDays)} jours"
                }
            } else {
                when {
                    remainingDays < 0.5 -> "under 1 day"
                    remainingDays < 1.5 -> "about 1 day"
                    else -> "about ${Math.round(remainingDays)} days"
                }
            }
            if (isFr) {
                "Il reste ${remainingKg.toInt()} kg, sous le seuil de ${thresholdKg.toInt()} kg. À ce rythme (${String.format(Locale.getDefault(), "%.1f", dailyRateKg)} kg/j), $formattedDays."
            } else {
                "${remainingKg.toInt()} kg remaining, below threshold of ${thresholdKg.toInt()} kg. At this rate (${String.format(Locale.getDefault(), "%.1f", dailyRateKg)} kg/d), $formattedDays."
            }
        } else {
            if (isFr) {
                "Il reste ${remainingKg.toInt()} kg, sous le seuil de ${thresholdKg.toInt()} kg. Pensez à réapprovisionner ce stock."
            } else {
                "${remainingKg.toInt()} kg remaining, below threshold of ${thresholdKg.toInt()} kg. Consider replenishing stock."
            }
        }

        binding.tvWarningSubtitle.text = forecastText
    }

    private fun parseDateToCalendar(dateStr: String?): Calendar {
        if (dateStr.isNullOrBlank()) {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
        val trimmed = dateStr.trim()
        val formats = arrayOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE),
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.FRANCE),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE),
            SimpleDateFormat("dd-MM-yyyy", Locale.FRANCE)
        )
        for (fmt in formats) {
            try {
                fmt.isLenient = false
                val date = fmt.parse(trimmed)
                if (date != null) {
                    return Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun getDayKey(dateStr: String): String {
        val cal = parseDateToCalendar(dateStr)
        return SimpleDateFormat("yyyyMMdd", Locale.FRANCE).format(cal.time)
    }

    private fun updateConsumptionUI(distributions: List<FeedDistributionEntity>) {
        if (_binding == null) return

        val dayLabelFormat = SimpleDateFormat("EEE", Locale.FRANCE)
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.FRANCE)

        // Build list of last 7 days from J-6 to Today (J-0)
        val last7Days = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dayKey = dayKeyFormat.format(cal.time)
            val label = if (daysAgo == 0) {
                "Aujourd'hui"
            } else {
                dayLabelFormat.format(cal.time).replaceFirstChar { it.uppercase(Locale.FRANCE) }
            }
            Triple(cal, dayKey, label)
        }

        val dailySums = last7Days.map { (_, dayKey, _) ->
            distributions.filter { dist ->
                getDayKey(dist.distributionDate) == dayKey
            }.sumOf { it.quantityKg }
        }

        // Bind 7-day text rows
        val dayLabelTvs = arrayOf(
            binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label,
            binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label,
            binding.tvDay7Label
        )
        val dayValueTvs = arrayOf(
            binding.tvDay1Value, binding.tvDay2Value, binding.tvDay3Value,
            binding.tvDay4Value, binding.tvDay5Value, binding.tvDay6Value,
            binding.tvDay7Value
        )
        val dayProgressBars = arrayOf(
            binding.pbDay1, binding.pbDay2, binding.pbDay3,
            binding.pbDay4, binding.pbDay5, binding.pbDay6,
            binding.pbDay7
        )

        val maxDailyKg = dailySums.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val dateFormatDisplay = SimpleDateFormat("dd/MM", Locale.FRANCE)

        for (i in 0 until 7) {
            val (cal, _, defaultLabel) = last7Days[i]
            val kgVal = dailySums[i]

            val dateStr = dateFormatDisplay.format(cal.time)
            val labelText = if (i == 6) "Aujourd'hui ($dateStr)" else "$defaultLabel ($dateStr)"
            dayLabelTvs[i].text = labelText

            val formattedVal = if (kgVal == 0.0) {
                "0 kg"
            } else if (kgVal % 1.0 == 0.0) {
                "${kgVal.toInt()} kg"
            } else {
                String.format(Locale.FRANCE, "%.1f kg", kgVal)
            }
            dayValueTvs[i].text = formattedVal

            val targetProgress = ((kgVal / maxDailyKg) * 100).toInt()
            animateProgressBar(dayProgressBars[i], targetProgress)
        }

        // Summary Statistics (Last 7 Days)
        val total7DaysKg = dailySums.sum()
        val formattedTotal = if (total7DaysKg % 1.0 == 0.0) {
            "${total7DaysKg.toInt()} kg"
        } else {
            String.format(Locale.FRANCE, "%.1f kg", total7DaysKg)
        }
        binding.tv7DaysTotalKg.text = "$formattedTotal consommés"

        val activeDaysCount = dailySums.count { it > 0.0 }.coerceAtLeast(1)
        val avgDaily = if (total7DaysKg > 0.0) total7DaysKg / activeDaysCount else 0.0
        binding.tvDailyAverageKg.text = if (avgDaily % 1.0 == 0.0) "${avgDaily.toInt()} kg" else String.format(Locale.FRANCE, "%.1f kg", avgDaily)

        // Breakdown by Feed Type
        val granulesKg = distributions.filter {
            val name = it.feedName.trim().lowercase(Locale.FRANCE)
            name.contains("granul")
        }.sumOf { it.quantityKg }

        val foinKg = distributions.filter {
            val name = it.feedName.trim().lowercase(Locale.FRANCE)
            name.contains("foin")
        }.sumOf { it.quantityKg }

        val legumesKg = distributions.filter {
            val name = it.feedName.trim().lowercase(Locale.FRANCE)
            name.contains("legum") || name.contains("légum")
        }.sumOf { it.quantityKg }

        val grainesKg = distributions.filter {
            val name = it.feedName.trim().lowercase(Locale.FRANCE)
            name.contains("graine")
        }.sumOf { it.quantityKg }

        fun formatKg(kg: Double): String {
            return if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else String.format(Locale.FRANCE, "%.1f kg", kg)
        }

        binding.tvConsumptionGranulesKg.text = formatKg(granulesKg)
        binding.tvConsumptionFoinKg.text = formatKg(foinKg)
        binding.tvConsumptionLegumesKg.text = formatKg(legumesKg)
        binding.tvConsumptionGrainesKg.text = formatKg(grainesKg)

        val maxTypeKg = maxOf(granulesKg, foinKg, legumesKg, grainesKg, 1.0)
        animateProgressBar(binding.pbGranules, ((granulesKg / maxTypeKg) * 100).toInt())
        animateProgressBar(binding.pbFoin, ((foinKg / maxTypeKg) * 100).toInt())
        animateProgressBar(binding.pbLegumes, ((legumesKg / maxTypeKg) * 100).toInt())
        animateProgressBar(binding.pbGraines, ((grainesKg / maxTypeKg) * 100).toInt())
    }

    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int) {
        val animation = android.animation.ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animation.duration = 650
        animation.interpolator = android.view.animation.DecelerateInterpolator()
        animation.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
