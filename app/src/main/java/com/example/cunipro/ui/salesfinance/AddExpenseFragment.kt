package com.example.cunipro.ui.salesfinance

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.databinding.FragmentAddExpenseBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        SalesFinanceViewModelFactory(app.financeRepository, app.animalRepository)
    }

    private var selectedCategory = "ACHAT_ALIMENT" // "ACHAT_ALIMENT", "SOINS", "AUTRE"
    private var selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCategorySelector()
        setupDatePicker()
        setupSubmitButton()

        selectCategory("ACHAT_ALIMENT")
    }

    private fun setupCategorySelector() {
        binding.btnCategoryFeed.setOnClickListener { selectCategory("ACHAT_ALIMENT") }
        binding.btnCategoryHealth.setOnClickListener { selectCategory("SOINS") }
        binding.btnCategoryOther.setOnClickListener { selectCategory("AUTRE") }
    }

    private fun selectCategory(cat: String) {
        selectedCategory = cat
        val primaryGreen = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)

        val setButtonSelected = { btn: com.google.android.material.button.MaterialButton, isSel: Boolean ->
            if (isSel) {
                btn.backgroundTintList = ColorStateList.valueOf(primaryGreen)
                btn.setTextColor(Color.WHITE)
                btn.elevation = 2f * resources.displayMetrics.density
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                btn.setTextColor(Color.parseColor("#4B5563"))
                btn.elevation = 0f
            }
            btn.strokeWidth = 0
        }

        setButtonSelected(binding.btnCategoryFeed, cat == "ACHAT_ALIMENT")
        setButtonSelected(binding.btnCategoryHealth, cat == "SOINS")
        setButtonSelected(binding.btnCategoryOther, cat == "AUTRE")

        when (cat) {
            "ACHAT_ALIMENT" -> {
                binding.tilLabel.hint = getString(R.string.expense_feed_hint)
                binding.etLabel.hint = null
                binding.tvExpenseInfoText.text = getString(R.string.expense_feed_info)
            }
            "SOINS" -> {
                binding.tilLabel.hint = getString(R.string.expense_health_hint)
                binding.etLabel.hint = null
                binding.tvExpenseInfoText.text = getString(R.string.expense_health_info)
            }
            "AUTRE" -> {
                binding.tilLabel.hint = getString(R.string.expense_other_hint)
                binding.etLabel.hint = null
                binding.tvExpenseInfoText.text = getString(R.string.expense_other_info)
            }
        }
    }

    private fun setupDatePicker() {
        binding.etExpenseDate.setText(BreedingDateCalculator.formatDateDisplay(selectedDateFormatted))

        binding.etExpenseDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                    binding.etExpenseDate.setText(BreedingDateCalculator.formatDateDisplay(selectedDateFormatted))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitExpense.setOnClickListener {
            val isFr = LanguageManager.isFrench(requireContext())
            val labelText = binding.etLabel.text?.toString()?.trim().orEmpty()
            if (labelText.isBlank()) {
                binding.etLabel.error = if (isFr) "Veuillez préciser le libellé" else "Please specify description"
                return@setOnClickListener
            }

            val amountText = binding.etAmount.text?.toString()?.trim()
            if (amountText.isNullOrBlank()) {
                binding.etAmount.error = if (isFr) "Veuillez entrer le montant" else "Please enter amount"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etAmount.error = if (isFr) "Montant invalide" else "Invalid amount"
                return@setOnClickListener
            }

            val supplierNote = binding.etSupplierNote.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

            viewModel.recordExpense(
                category = selectedCategory,
                amount = amount,
                date = selectedDateFormatted,
                description = labelText,
                supplierOrNote = supplierNote
            )

            Toast.makeText(requireContext(), if (isFr) "Dépense enregistrée !" else "Expense recorded!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
