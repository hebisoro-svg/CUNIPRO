package com.example.cunipro.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.databinding.FragmentDashboardBinding
import com.example.cunipro.ui.adapter.TaskAdapter
import com.example.cunipro.util.setupTopActions
import com.example.cunipro.util.updateTopNotificationBadge
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        DashboardViewModelFactory(
            app.animalRepository,
            app.reproductionRepository,
            app.feedRepository,
            app.financeRepository,
            app.taskRepository,
            app.notificationRepository
        )
    }

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
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
        observeData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { _, _ -> }
        binding.rvDashboardTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun observeData() {
        // 1. Cheptel & Lapins
        viewModel.activeAnimalCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalAnimalsCount.text = (count ?: 0).toString()
        }

        var males = 0
        var females = 0
        val updateSexText = {
            binding.tvSexBreakdown.text = "Mâles: $males | Femelles: $females"
        }

        viewModel.maleCount.observe(viewLifecycleOwner) { m ->
            males = m ?: 0
            updateSexText()
        }

        viewModel.femaleCount.observe(viewLifecycleOwner) { f ->
            females = f ?: 0
            updateSexText()
        }

        viewModel.reproductionCategoryCount.observe(viewLifecycleOwner) { count ->
            binding.tvReproCount.text = (count ?: 0).toString()
        }

        viewModel.weanedCategoryCount.observe(viewLifecycleOwner) { count ->
            binding.tvSevreCount.text = (count ?: 0).toString()
        }

        viewModel.fatteningCategoryCount.observe(viewLifecycleOwner) { count ->
            binding.tvEngraissementCount.text = (count ?: 0).toString()
        }

        viewModel.primipareCategoryCount.observe(viewLifecycleOwner) { count ->
            binding.tvPrimipareCount.text = (count ?: 0).toString()
        }

        viewModel.multipareCategoryCount.observe(viewLifecycleOwner) { count ->
            binding.tvMultipareCount.text = (count ?: 0).toString()
        }

        viewModel.soldAnimalCount.observe(viewLifecycleOwner) { count ->
            binding.tvVenduCount.text = (count ?: 0).toString()
        }

        // 2. Mortalité
        viewModel.deceasedAnimalCount.observe(viewLifecycleOwner) { count ->
            binding.tvMortalityCount.text = "${count ?: 0} décès"
        }

        viewModel.mortalityRate.observe(viewLifecycleOwner) { rate ->
            val safeRate = rate ?: 0.0
            binding.tvMortalityRate.text = String.format(Locale.getDefault(), "Taux: %.1f %%", safeRate)
        }

        // 3. Finances & Revenus
        viewModel.totalIncome.observe(viewLifecycleOwner) { inc ->
            val income = inc ?: 0.0
            binding.tvTotalIncome.text = String.format(Locale.getDefault(), "%,.0f FCFA", income)
        }

        viewModel.totalExpenses.observe(viewLifecycleOwner) { exp ->
            val expenses = exp ?: 0.0
            binding.tvTotalExpenses.text = String.format(Locale.getDefault(), "%,.0f FCFA", expenses)
        }

        viewModel.netBalance.observe(viewLifecycleOwner) { bal ->
            val balance = bal ?: 0.0
            binding.tvNetBalance.text = String.format(Locale.getDefault(), "%,.0f FCFA", balance)
        }

        // 4. Alimentation & Stock
        viewModel.totalFeedStockKg.observe(viewLifecycleOwner) { stock ->
            val safeStock = stock ?: 0.0
            binding.tvFeedStockKg.text = String.format(Locale.getDefault(), "%.1f kg", safeStock)
        }

        viewModel.lowStockAlertCount.observe(viewLifecycleOwner) { alerts ->
            val count = alerts ?: 0
            binding.tvLowStockCount.text = "$count alerte(s)"
        }

        // 5. Reproduction & Naissances
        viewModel.activeReproductionCount.observe(viewLifecycleOwner) { count ->
            binding.tvActiveGestationsCount.text = (count ?: 0).toString()
        }

        viewModel.totalKitsBornAlive.observe(viewLifecycleOwner) { count ->
            binding.tvKitsBornCount.text = (count ?: 0).toString()
        }

        viewModel.totalKitsBornDead.observe(viewLifecycleOwner) { count ->
            binding.tvKitsBornDeadCount.text = (count ?: 0).toString()
        }

        viewModel.totalKitsWeaned.observe(viewLifecycleOwner) { count ->
            binding.tvKitsWeanedCount.text = (count ?: 0).toString()
        }

        // 6. Tâches & Badge de notifications
        viewModel.pendingTaskCount.observe(viewLifecycleOwner) { count ->
            val safeCount = count ?: 0
            binding.tvPendingTasksCount.text = "$safeCount en attente"
        }

        viewModel.totalNotificationBadgeCount.observe(viewLifecycleOwner) { count ->
            val safeCount = count ?: 0
            updateTopNotificationBadge(binding.includeTopActions.tvNotificationBadge, safeCount)
        }

        viewModel.pendingTasks.observe(viewLifecycleOwner) { tasks ->
            val taskList = tasks ?: emptyList()
            if (taskList.isEmpty()) {
                binding.rvDashboardTasks.visibility = View.GONE
                binding.tvEmptyTasks.visibility = View.VISIBLE
            } else {
                binding.rvDashboardTasks.visibility = View.VISIBLE
                binding.tvEmptyTasks.visibility = View.GONE
                taskAdapter.submitList(taskList.take(5))
            }
        }
    }

    private fun setupListeners() {
        binding.btnQuickAddAnimal.setOnClickListener {
            findNavController().navigate(R.id.animalsFragment)
        }

        binding.btnQuickNewMating.setOnClickListener {
            findNavController().navigate(R.id.reproductionFragment)
        }

        binding.btnQuickFeed.setOnClickListener {
            findNavController().navigate(R.id.feedFragment)
        }

        binding.btnQuickFinance.setOnClickListener {
            findNavController().navigate(R.id.salesFinanceFragment)
        }

        binding.tvPendingTasksCount.setOnClickListener {
            findNavController().navigate(R.id.tasksFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
