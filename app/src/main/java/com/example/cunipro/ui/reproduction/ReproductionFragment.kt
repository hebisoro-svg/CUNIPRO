package com.example.cunipro.ui.reproduction

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.ReproductionEntity
import com.example.cunipro.databinding.FragmentReproductionBinding
import com.example.cunipro.ui.adapter.ReproductionAdapter
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupTopActions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReproductionFragment : Fragment() {

    private var _binding: FragmentReproductionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReproductionViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        ReproductionViewModelFactory(app.reproductionRepository)
    }

    private lateinit var reproductionAdapter: ReproductionAdapter
    private var allReproductionsList: List<ReproductionEntity> = emptyList()
    private var currentTab = ReproductionAdapter.TAB_GESTATIONS

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReproductionBinding.inflate(inflater, container, false)
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
        setupTabFilter()
        observeKpis()
        observeData()

        binding.fabNewMating.setOnClickListener {
            findNavController().navigate(R.id.action_reproductionFragment_to_addMatingFragment)
        }
    }

    private fun setupRecyclerView() {
        reproductionAdapter = ReproductionAdapter(
            onKindlingClick = { repro ->
                val bundle = Bundle().apply {
                    putLong("reproductionId", repro.id)
                }
                findNavController().navigate(
                    R.id.action_reproductionFragment_to_recordKindlingFragment,
                    bundle
                )
            },
            onWeaningClick = { repro ->
                val todayIso = com.example.cunipro.util.BreedingDateCalculator.getTodayString()
                viewModel.recordWeaning(repro, todayIso, repro.bornAliveCount)
                Toast.makeText(requireContext(), "Portée sevrée avec succès !", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvReproductions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reproductionAdapter
        }
    }

    private fun setupTabFilter() {
        binding.btnTabGestations.setOnClickListener {
            selectTab(ReproductionAdapter.TAB_GESTATIONS)
        }
        binding.btnTabMisesBas.setOnClickListener {
            selectTab(ReproductionAdapter.TAB_MISES_BAS)
        }
        binding.btnTabSevrages.setOnClickListener {
            selectTab(ReproductionAdapter.TAB_SEVRAGES)
        }

        updateTabButtonsStyle()
    }

    private fun selectTab(tab: Int) {
        if (currentTab != tab) {
            currentTab = tab
            updateTabButtonsStyle()
            filterListBySelectedTab()
        }
    }

    private fun updateTabButtonsStyle() {
        if (_binding == null) return

        val activeBgColor = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val activeTextColor = Color.WHITE
        val inactiveBgColor = Color.TRANSPARENT
        val inactiveTextColor = Color.parseColor("#424242")

        fun applyStyle(
            button: com.google.android.material.button.MaterialButton,
            isActive: Boolean
        ) {
            button.backgroundTintList = ColorStateList.valueOf(if (isActive) activeBgColor else inactiveBgColor)
            button.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
            button.paint.isFakeBoldText = isActive
        }

        applyStyle(binding.btnTabGestations, currentTab == ReproductionAdapter.TAB_GESTATIONS)
        applyStyle(binding.btnTabMisesBas, currentTab == ReproductionAdapter.TAB_MISES_BAS)
        applyStyle(binding.btnTabSevrages, currentTab == ReproductionAdapter.TAB_SEVRAGES)
    }

    private fun observeKpis() {
        viewModel.activeReproductionCount.observe(viewLifecycleOwner) { count ->
            val safeCount = count ?: 0
            val isFr = LanguageManager.isFrench(requireContext())
            binding.tvKpiGestationsCount.text = safeCount.toString()
            binding.tvActiveGestationsSubtitle.text = if (isFr) "$safeCount gestations en cours" else "$safeCount active gestations"
        }

        viewModel.totalKitsBornAlive.observe(viewLifecycleOwner) { count ->
            binding.tvKpiBornAliveCount.text = (count ?: 0).toString()
        }

        viewModel.successRate.observe(viewLifecycleOwner) { rate ->
            val safeRate = rate ?: 0.0
            binding.tvKpiSuccessRate.text = String.format(Locale.getDefault(), "%.0f %%", safeRate)
        }
    }

    private fun observeData() {
        viewModel.allReproductions.observe(viewLifecycleOwner) { list ->
            allReproductionsList = list
            updateTabCounts()
            filterListBySelectedTab()
        }
    }

    private fun getDaysUntilKindling(repro: ReproductionEntity): Int {
        val matingDateStr = repro.matingDate
        if (matingDateStr.isBlank()) return 31
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val date = sdf.parse(matingDateStr) ?: return 31
            val diff = Date().time - date.time
            val daysElapsed = (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            31 - daysElapsed
        } catch (_: Exception) {
            31
        }
    }

    private fun updateTabCounts() {
        if (_binding == null) return

        val isFr = LanguageManager.isFrench(requireContext())

        val gestations = allReproductionsList.filter { it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" }
        val kindlingGestations = allReproductionsList.filter {
            it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" && getDaysUntilKindling(it) <= 5
        }
        val recordedKindlings = allReproductionsList.filter { !it.actualKindlingDate.isNullOrBlank() }
        val totalMisesBas = kindlingGestations.size + recordedKindlings.size

        val weanings = allReproductionsList.filter { !it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" }

        binding.btnTabGestations.text = if (isFr) "Gestations · ${gestations.size}" else "Gestations · ${gestations.size}"
        binding.btnTabMisesBas.text = if (isFr) "Mises bas · $totalMisesBas" else "Kindling · $totalMisesBas"
        binding.btnTabSevrages.text = if (isFr) "Sevrages · ${weanings.size}" else "Weaning · ${weanings.size}"
    }

    private fun filterListBySelectedTab() {
        if (_binding == null) return

        reproductionAdapter.currentTab = currentTab

        when (currentTab) {
            ReproductionAdapter.TAB_MISES_BAS -> {
                val kindlingGestations = allReproductionsList.filter {
                    it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" && getDaysUntilKindling(it) <= 5
                }
                val recordedKindlings = allReproductionsList.filter { !it.actualKindlingDate.isNullOrBlank() }
                val fullKindlingList = kindlingGestations + recordedKindlings

                reproductionAdapter.submitList(fullKindlingList)

                val pendingKindling = kindlingGestations.minByOrNull { getDaysUntilKindling(it) }
                if (pendingKindling != null) {
                    binding.cardKindlingAlertBanner.visibility = View.VISIBLE
                    val daysLeft = getDaysUntilKindling(pendingKindling)
                    val isFr = LanguageManager.isFrench(requireContext())
                    binding.tvAlertBannerTitle.text = when {
                        daysLeft > 1 -> if (isFr) "Mise bas attendue dans $daysLeft jours" else "Kindling expected in $daysLeft days"
                        daysLeft == 1 -> if (isFr) "Mise bas attendue dans 1 jour" else "Kindling expected in 1 day"
                        daysLeft == 0 -> if (isFr) "Mise bas attendue aujourd'hui" else "Kindling expected today"
                        else -> if (isFr) "Mise bas attendue (en retard)" else "Kindling expected (overdue)"
                    }
                    val expectedDateDisp = formatDisplayDate(pendingKindling.expectedKindlingDate ?: pendingKindling.matingDate)
                    val expectedStr = if (isFr) "prévue le" else "due on"
                    val unknownStr = if (isFr) "Inconnu" else "Unknown"
                    binding.tvAlertBannerSubtitle.text = "${pendingKindling.femaleTag} × ${pendingKindling.maleTag ?: unknownStr} · $expectedStr $expectedDateDisp"
                    binding.btnQuickRecordKindling.setOnClickListener {
                        val bundle = Bundle().apply {
                            putLong("reproductionId", pendingKindling.id)
                        }
                        findNavController().navigate(
                            R.id.action_reproductionFragment_to_recordKindlingFragment,
                            bundle
                        )
                    }
                } else {
                    binding.cardKindlingAlertBanner.visibility = View.GONE
                }
            }
            ReproductionAdapter.TAB_SEVRAGES -> {
                binding.cardKindlingAlertBanner.visibility = View.GONE
                val weanings = allReproductionsList.filter { !it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" }
                reproductionAdapter.submitList(weanings)
            }
            else -> { // TAB_GESTATIONS
                binding.cardKindlingAlertBanner.visibility = View.GONE
                val gestations = allReproductionsList.filter { it.actualKindlingDate.isNullOrBlank() && it.status == "EN_COURS" }
                reproductionAdapter.submitList(gestations)
            }
        }
    }

    private fun formatDisplayDate(isoDateStr: String?): String {
        if (isoDateStr.isNullOrBlank()) return "-"
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val displayFormat = SimpleDateFormat("d MMM yyyy", Locale.FRANCE)
            val date = isoFormat.parse(isoDateStr)
            if (date != null) displayFormat.format(date) else isoDateStr
        } catch (_: Exception) {
            isoDateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
