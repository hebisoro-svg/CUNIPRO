package com.example.cunipro.ui.healthfinance

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.databinding.FragmentHealthFinanceBinding
import com.example.cunipro.ui.adapter.HealthAdapter
import com.example.cunipro.ui.adapter.MortalityAdapter
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupTopActions

class HealthFinanceFragment : Fragment() {

    private var _binding: FragmentHealthFinanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        HealthFinanceViewModelFactory(app.healthRepository, app.financeRepository, app.animalRepository)
    }

    private lateinit var healthAdapter: HealthAdapter
    private lateinit var mortalityAdapter: MortalityAdapter

    private var currentAnimals: List<AnimalEntity> = emptyList()
    private var isMortalityTabActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthFinanceBinding.inflate(inflater, container, false)
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

        viewModel.checkExpiredCareLogs()

        setupRecyclerViews()
        setupTabsAndButtons()
        observeData()
    }

    private fun setupRecyclerViews() {
        healthAdapter = HealthAdapter { log ->
            viewModel.toggleCareStatus(log)
        }
        binding.rvHealthLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = healthAdapter
        }

        mortalityAdapter = MortalityAdapter()
        binding.rvMortalityLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mortalityAdapter
        }
    }

    private fun setupTabsAndButtons() {
        binding.btnTabHistory.setOnClickListener {
            selectTab(isMortality = false)
        }

        binding.btnTabMortality.setOnClickListener {
            selectTab(isMortality = true)
        }

        binding.btnActionAddCare.setOnClickListener {
            findNavController().navigate(R.id.action_healthFinanceFragment_to_addCareFragment)
        }

        binding.btnActionAddMortality.setOnClickListener {
            findNavController().navigate(R.id.action_healthFinanceFragment_to_addMortalityFragment)
        }

        // Sync tab state and UI on view creation / recreation
        selectTab(isMortality = isMortalityTabActive)
    }

    private fun selectTab(isMortality: Boolean) {
        isMortalityTabActive = isMortality
        val primaryGreen = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val textPrimary = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.cuni_text_primary)

        if (isMortality) {
            binding.layoutCareTab.visibility = View.GONE
            binding.layoutMortalityTab.visibility = View.VISIBLE

            binding.btnTabMortality.backgroundTintList = ColorStateList.valueOf(primaryGreen)
            binding.btnTabMortality.setTextColor(Color.WHITE)

            binding.btnTabHistory.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            binding.btnTabHistory.setTextColor(textPrimary)
        } else {
            binding.layoutCareTab.visibility = View.VISIBLE
            binding.layoutMortalityTab.visibility = View.GONE

            binding.btnTabHistory.backgroundTintList = ColorStateList.valueOf(primaryGreen)
            binding.btnTabHistory.setTextColor(Color.WHITE)

            binding.btnTabMortality.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            binding.btnTabMortality.setTextColor(textPrimary)
        }
    }

    private fun observeData() {
        viewModel.treatments.observe(viewLifecycleOwner) { treatments ->
            healthAdapter.submitList(treatments)
            binding.tvEmptyHealthLogs.visibility = if (treatments.isEmpty()) View.VISIBLE else View.GONE
            updateSummaryHeader()
        }

        viewModel.mortalityLogs.observe(viewLifecycleOwner) { mortalities ->
            mortalityAdapter.submitList(mortalities)
            binding.tvEmptyMortalityLogs.visibility = if (mortalities.isEmpty()) View.VISIBLE else View.GONE
            updateMortalityKpis(mortalities)
            updateSummaryHeader()
        }

        viewModel.upcomingReminder.observe(viewLifecycleOwner) { reminder ->
            if (reminder != null && !reminder.nextDueDate.isNullOrBlank()) {
                binding.cardReminderBanner.visibility = View.VISIBLE
                val isFr = LanguageManager.isFrench(requireContext())
                val formatted = BreedingDateCalculator.formatDateDisplay(reminder.nextDueDate)
                val disease = if (reminder.diseaseName.isNotBlank()) reminder.diseaseName else reminder.treatmentName
                val defaultTarget = if (isFr) "Élevage" else "Herd"
                val target = reminder.animalTag ?: defaultTarget
                binding.tvReminderTitle.text = if (isFr) "Prochain soin prévu" else "Next scheduled care"
                binding.tvReminderSubtitle.text = "$disease · $target · $formatted"
            } else {
                binding.cardReminderBanner.visibility = View.GONE
            }
        }

        viewModel.allAnimals.observe(viewLifecycleOwner) { animals ->
            currentAnimals = animals
            updateMortalityKpis(mortalityAdapter.currentList)
        }
    }

    private fun updateSummaryHeader() {
        if (_binding == null) return
        val isFr = LanguageManager.isFrench(requireContext())
        val activeCareCount = healthAdapter.currentList.count { it.status.equals("EN_COURS", ignoreCase = true) }
        val mortalityCount = mortalityAdapter.currentList.sumOf { it.deathCount }

        val careStr = if (isFr) "soin(s) en cours" else "active care(s)"
        val deathStr = if (isFr) "décès" else "deaths"
        binding.tvHeaderSubtitleSummary.text = "$activeCareCount $careStr · $mortalityCount $deathStr"
    }

    private fun updateMortalityKpis(mortalities: List<HealthLogEntity>) {
        if (_binding == null) return

        val totalDeaths = mortalities.sumOf { it.deathCount }
        binding.tvMortalityMonthCount.text = totalDeaths.toString()

        val totalLiving = currentAnimals.count { it.isLiving() }
        val totalRecorded = totalLiving + totalDeaths
        val rate = if (totalRecorded > 0) (totalDeaths.toDouble() / totalRecorded.toDouble() * 100).toInt() else 0
        binding.tvMortalityRatePercent.text = "$rate %"

        // Group by cause for stats
        val causeMap = mutableMapOf<String, Int>()
        mortalities.forEach { log ->
            val causeKey = log.deathCause?.trim()?.lowercase() ?: "autre"
            causeMap[causeKey] = (causeMap[causeKey] ?: 0) + log.deathCauseCountOrOne(log)
        }

        val mainCause = causeMap.maxByOrNull { it.value }?.key?.capitalizeFirstLetter() ?: "Maladie"
        binding.tvMortalityMainCause.text = mainCause

        // Progress bars
        val maladieCount = causeMap.entries.filter { it.key.contains("maladie") }.sumOf { it.value }
        val mortNesCount = causeMap.entries.filter { it.key.contains("mort") || it.key.contains("né") }.sumOf { it.value }
        val chaleurCount = causeMap.entries.filter { it.key.contains("chaleur") }.sumOf { it.value }
        val accidentCount = causeMap.entries.filter { it.key.contains("accident") }.sumOf { it.value }

        binding.tvCountMaladie.text = maladieCount.toString()
        binding.progressMaladie.progress = maladieCount

        binding.tvCountMortNes.text = mortNesCount.toString()
        binding.progressMortNes.progress = mortNesCount

        binding.tvCountChaleur.text = chaleurCount.toString()
        binding.progressChaleur.progress = chaleurCount

        binding.tvCountAccident.text = accidentCount.toString()
        binding.progressAccident.progress = accidentCount
    }

    private fun HealthLogEntity.deathCauseCountOrOne(log: HealthLogEntity): Int = if (log.deathCount > 0) log.deathCount else 1

    private fun String.capitalizeFirstLetter(): String =
        if (isEmpty()) this else substring(0, 1).uppercase() + substring(1)

    private fun AnimalEntity.isLiving(): Boolean {
        return !status.equals("DECEDE", ignoreCase = true) &&
               !status.equals("DÉCÉDÉ", ignoreCase = true) &&
               !status.equals("VENDU", ignoreCase = true) &&
               !category.contains("DECED", ignoreCase = true) &&
               !category.equals("VENDU", ignoreCase = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
