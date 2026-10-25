package com.example.cunipro.ui.salesfinance

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.example.cunipro.databinding.FragmentSalesFinanceBinding
import com.example.cunipro.util.setupTopActions
import java.util.Locale

class SalesFinanceFragment : Fragment() {

    private var _binding: FragmentSalesFinanceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        SalesFinanceViewModelFactory(app.financeRepository, app.animalRepository)
    }

    private lateinit var salesAdapter: SalesAdapter
    private lateinit var expenseAdapter: ExpenseAdapter

    private var isExpensesTabActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesFinanceBinding.inflate(inflater, container, false)
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

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerViews()
        setupTabsAndButtons()
        observeData()
    }

    private fun setupRecyclerViews() {
        salesAdapter = SalesAdapter()
        binding.rvSalesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }

        expenseAdapter = ExpenseAdapter()
        binding.rvExpensesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun setupTabsAndButtons() {
        binding.btnTabSales.setOnClickListener {
            selectTab(isExpenses = false)
        }

        binding.btnTabExpenses.setOnClickListener {
            selectTab(isExpenses = true)
        }

        binding.btnBottomAction.setOnClickListener {
            if (isExpensesTabActive) {
                findNavController().navigate(R.id.action_salesFinanceFragment_to_addExpenseFragment)
            } else {
                findNavController().navigate(R.id.action_salesFinanceFragment_to_addSaleFragment)
            }
        }

        // Sync tab state and UI on view creation / recreation
        selectTab(isExpenses = isExpensesTabActive)
    }

    private fun selectTab(isExpenses: Boolean) {
        isExpensesTabActive = isExpenses
        val primaryGreen = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.cuni_text_primary)

        if (isExpenses) {
            binding.layoutSalesTab.visibility = View.GONE
            binding.layoutExpensesTab.visibility = View.VISIBLE

            binding.btnTabExpenses.backgroundTintList = ColorStateList.valueOf(primaryGreen)
            binding.btnTabExpenses.setTextColor(Color.WHITE)

            binding.btnTabSales.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            binding.btnTabSales.setTextColor(textPrimary)

            binding.btnBottomAction.text = "+ Nouvelle dépense"
        } else {
            binding.layoutSalesTab.visibility = View.VISIBLE
            binding.layoutExpensesTab.visibility = View.GONE

            binding.btnTabSales.backgroundTintList = ColorStateList.valueOf(primaryGreen)
            binding.btnTabSales.setTextColor(Color.WHITE)

            binding.btnTabExpenses.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            binding.btnTabExpenses.setTextColor(textPrimary)

            binding.btnBottomAction.text = "+ Nouvelle vente"
        }
    }

    private fun observeData() {
        // Summary KPIs
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

        // Sales List
        viewModel.salesLogs.observe(viewLifecycleOwner) { sales ->
            salesAdapter.submitList(sales)
            binding.tvEmptySales.visibility = if (sales.isEmpty()) View.VISIBLE else View.GONE
            binding.btnTabSales.text = "Ventes · ${sales.size}"
        }

        // Expenses List
        viewModel.expenseLogs.observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.submitList(expenses)
            binding.tvEmptyExpenses.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            binding.btnTabExpenses.text = "Dépenses · ${expenses.size}"
        }

        // Expense Breakdown Bars
        viewModel.expenseBreakdown.observe(viewLifecycleOwner) { breakdown ->
            binding.tvAmountAlimentation.text = String.format(Locale.getDefault(), "%,.0f FCFA", breakdown.feedAmount)
            binding.tvAmountSante.text = String.format(Locale.getDefault(), "%,.0f FCFA", breakdown.healthAmount)
            binding.tvAmountAutresCharges.text = String.format(Locale.getDefault(), "%,.0f FCFA", breakdown.otherAmount)

            val total = if (breakdown.totalExpenses > 0) breakdown.totalExpenses else 1.0
            binding.progressAlimentation.max = total.toInt()
            binding.progressAlimentation.progress = breakdown.feedAmount.toInt()

            binding.progressSante.max = total.toInt()
            binding.progressSante.progress = breakdown.healthAmount.toInt()

            binding.progressAutresCharges.max = total.toInt()
            binding.progressAutresCharges.progress = breakdown.otherAmount.toInt()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
