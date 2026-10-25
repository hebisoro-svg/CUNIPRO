package com.example.cunipro.ui.feed

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.FeedStockEntity
import com.example.cunipro.databinding.FragmentRecordFeedConsumptionBinding
import com.example.cunipro.util.setupAutoRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecordFeedConsumptionFragment : Fragment() {

    private var _binding: FragmentRecordFeedConsumptionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        FeedViewModelFactory(app.feedRepository)
    }

    private var selectedFeedName: String = "Granulés"
    private var consumptionQtyKg: Double = 0.0
    private var currentStockList: List<FeedStockEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordFeedConsumptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passedFeedName = arguments?.getString("feedName")
        if (!passedFeedName.isNullOrBlank()) {
            selectedFeedName = passedFeedName
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackConsumption.setOnClickListener {
            findNavController().navigateUp()
        }

        val passedFeedName = arguments?.getString("feedName")
        if (!passedFeedName.isNullOrBlank()) {
            when (passedFeedName.lowercase(Locale.getDefault())) {
                "foin" -> binding.chipConsumptionFoin.isChecked = true
                "légumes", "legumes" -> binding.chipConsumptionLegumes.isChecked = true
                "graines" -> binding.chipConsumptionGraines.isChecked = true
                else -> binding.chipConsumptionGranules.isChecked = true
            }
        }

        setupFeedTypeChips()
        setupSteppers()
        setupDatePicker()
        setupSaveButton()

        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Calendar.getInstance().time)
        binding.etConsumptionDate.setText(todayStr)

        observeCurrentStock()
    }

    private fun observeCurrentStock() {
        viewModel.feedStockList.observe(viewLifecycleOwner) { list ->
            currentStockList = list
            updateConsumptionPreview()
        }
    }

    private fun normalizeFeedName(name: String): String {
        return name.lowercase(Locale.FRANCE)
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .trim()
    }

    private fun getAvailableStock(): Double {
        val target = normalizeFeedName(selectedFeedName)
        val existing = currentStockList.find { normalizeFeedName(it.feedName) == target }
        return existing?.quantityKg ?: 0.0
    }

    private fun setupFeedTypeChips() {
        binding.chipGroupConsumptionFeedType.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipConsumptionGranules
            selectedFeedName = when (checkedId) {
                R.id.chipConsumptionFoin -> "Foin"
                R.id.chipConsumptionLegumes -> "Légumes"
                R.id.chipConsumptionGraines -> "Graines"
                else -> "Granulés"
            }
            updateConsumptionPreview()
        }
    }

    private fun setupSteppers() {
        updateQtyDisplay()

        binding.btnMinusConsumptionQty.setupAutoRepeat {
            if (consumptionQtyKg >= 1.0) {
                consumptionQtyKg -= 1.0
                updateQtyDisplay()
            }
        }

        binding.btnPlusConsumptionQty.setupAutoRepeat {
            val availableStock = getAvailableStock()
            if (consumptionQtyKg + 1.0 <= availableStock) {
                consumptionQtyKg += 1.0
                updateQtyDisplay()
            } else {
                Toast.makeText(
                    requireContext(),
                    "La quantité ne peut pas dépasser le stock disponible (${availableStock.toInt()} kg)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateQtyDisplay() {
        if (_binding == null) return
        binding.tvConsumptionQtyCount.text = String.format(Locale.FRANCE, "%.0f kg", consumptionQtyKg)
        updateConsumptionPreview()
    }

    private fun updateConsumptionPreview() {
        if (_binding == null) return

        val availableStock = getAvailableStock()
        if (consumptionQtyKg > availableStock) {
            consumptionQtyKg = availableStock
            binding.tvConsumptionQtyCount.text = String.format(Locale.FRANCE, "%.0f kg", consumptionQtyKg)
        }

        val remainingKg = (availableStock - consumptionQtyKg).coerceAtLeast(0.0)

        binding.tvPreviewConsumptionFeedName.text = selectedFeedName
        binding.tvPreviewConsumptionCalculation.text = String.format(Locale.FRANCE, "%.0f kg → %.0f kg", availableStock, remainingKg)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val showPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(cal.time)
                    binding.etConsumptionDate.setText(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etConsumptionDate.setOnClickListener { showPicker() }
        binding.tilConsumptionDate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupSaveButton() {
        binding.btnSaveConsumption.setOnClickListener {
            val availableStock = getAvailableStock()

            if (availableStock <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "Aucun stock disponible pour $selectedFeedName (0 kg). Ajoutez d'abord du stock via le bouton '+ Ajout de stock'.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (consumptionQtyKg <= 0.0) {
                Toast.makeText(
                    requireContext(),
                    "Veuillez indiquer une quantité à enlever supérieure à 0 kg",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (consumptionQtyKg > availableStock) {
                Toast.makeText(
                    requireContext(),
                    "La quantité saisie (${consumptionQtyKg.toInt()} kg) dépasse le stock disponible (${availableStock.toInt()} kg)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val dateInput = binding.etConsumptionDate.text.toString().trim()
            val isoDate = parseInputToIso(dateInput)

            findNavController().previousBackStackEntry?.savedStateHandle?.set("showConsumptionTab", true)
            viewModel.recordConsumption(selectedFeedName, consumptionQtyKg, isoDate)
            Toast.makeText(
                requireContext(),
                "Sortie de stock enregistrée ($selectedFeedName -${consumptionQtyKg.toInt()} kg)",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun parseInputToIso(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            inputFormat.isLenient = false
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val date = inputFormat.parse(dateStr)
            if (date != null) isoFormat.format(date) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
