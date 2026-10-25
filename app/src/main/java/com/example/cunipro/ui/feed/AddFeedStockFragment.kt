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
import com.example.cunipro.databinding.FragmentAddFeedStockBinding
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupAutoRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddFeedStockFragment : Fragment() {

    private var _binding: FragmentAddFeedStockBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        FeedViewModelFactory(app.feedRepository)
    }

    private var selectedFeedName: String = "Granulés"
    private var addQtyKg: Double = 0.0
    private var thresholdKg: Double = 0.0
    private var currentStockList: List<FeedStockEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFeedStockBinding.inflate(inflater, container, false)
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

        binding.btnBackAddStock.setOnClickListener {
            findNavController().navigateUp()
        }

        setupFeedTypeChips()
        setupSteppers()
        setupDatePicker()
        setupSaveButton()

        val passedFeedName = arguments?.getString("feedName")
        if (!passedFeedName.isNullOrBlank()) {
            when (passedFeedName.lowercase(Locale.getDefault())) {
                "foin", "hay" -> binding.chipFoin.isChecked = true
                "légumes", "legumes", "vegetables" -> binding.chipLegumes.isChecked = true
                "graines", "seeds" -> binding.chipGraines.isChecked = true
                "autres", "others" -> binding.chipAutres.isChecked = true
                else -> binding.chipGranules.isChecked = true
            }
        }

        val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        binding.etAddDate.setText(todayStr)

        observeCurrentStock()
    }

    private fun observeCurrentStock() {
        viewModel.feedStockList.observe(viewLifecycleOwner) { list ->
            currentStockList = list
            updateStockPreview()
        }
    }

    private fun getAvailableStockAfterAddition(): Double {
        val existing = currentStockList.find { it.feedName.equals(selectedFeedName, ignoreCase = true) }
        val currentKg = existing?.quantityKg ?: 0.0
        return currentKg + addQtyKg
    }

    private fun getMaxThresholdKg(): Double {
        return getAvailableStockAfterAddition() * 0.90
    }

    private fun validateAndCapThreshold() {
        val maxThreshold = getMaxThresholdKg()
        if (thresholdKg > maxThreshold) {
            thresholdKg = maxThreshold
            updateThresholdDisplay()
        }
    }

    private fun setupFeedTypeChips() {
        binding.chipGroupFeedType.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipGranules
            selectedFeedName = when (checkedId) {
                R.id.chipFoin -> "Foin"
                R.id.chipLegumes -> "Légumes"
                R.id.chipGraines -> "Graines"
                R.id.chipAutres -> "Autres"
                else -> "Granulés"
            }
            updateStockPreview()
        }
    }

    private fun setupSteppers() {
        updateQtyDisplay()
        updateThresholdDisplay()

        binding.btnMinusQty.setupAutoRepeat {
            if (addQtyKg >= 1.0) {
                addQtyKg -= 1.0
                updateQtyDisplay()
                validateAndCapThreshold()
            }
        }

        binding.btnPlusQty.setupAutoRepeat {
            addQtyKg += 1.0
            updateQtyDisplay()
        }

        binding.btnMinusThreshold.setupAutoRepeat {
            if (thresholdKg >= 1.0) {
                thresholdKg -= 1.0
                updateThresholdDisplay()
            }
        }

        binding.btnPlusThreshold.setupAutoRepeat {
            val maxThreshold = getMaxThresholdKg()
            if (thresholdKg + 1.0 <= maxThreshold) {
                thresholdKg += 1.0
                updateThresholdDisplay()
            } else {
                val formattedMax = if (maxThreshold % 1.0 == 0.0) {
                    String.format(Locale.getDefault(), "%.0f", maxThreshold)
                } else {
                    String.format(Locale.getDefault(), "%.1f", maxThreshold)
                }
                val isFr = LanguageManager.isFrench(requireContext())
                val msg = if (isFr) "Le seuil d'alerte ne peut pas dépasser 90% du stock disponible ($formattedMax kg max)" else "Alert threshold cannot exceed 90% of available stock ($formattedMax kg max)"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateQtyDisplay() {
        if (_binding == null) return
        binding.tvAddQtyCount.text = String.format(Locale.getDefault(), "%.0f kg", addQtyKg)
        updateStockPreview()
    }

    private fun updateThresholdDisplay() {
        if (_binding == null) return
        val text = if (thresholdKg % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%.0f kg", thresholdKg)
        } else {
            String.format(Locale.getDefault(), "%.1f kg", thresholdKg)
        }
        binding.tvThresholdCount.text = text
    }

    private fun updateStockPreview() {
        if (_binding == null) return

        val existing = currentStockList.find { it.feedName.equals(selectedFeedName, ignoreCase = true) }
        val currentKg = existing?.quantityKg ?: 0.0
        val newKg = currentKg + addQtyKg

        val isFr = LanguageManager.isFrench(requireContext())
        val displayFeedName = when (selectedFeedName.lowercase(Locale.getDefault())) {
            "foin" -> if (isFr) "Foin" else "Hay"
            "légumes", "legumes" -> if (isFr) "Légumes" else "Vegetables"
            "graines" -> if (isFr) "Graines" else "Seeds"
            "autres" -> if (isFr) "Autres" else "Others"
            else -> if (isFr) "Granulés" else "Pellets"
        }

        binding.tvPreviewFeedName.text = displayFeedName
        binding.tvPreviewStockCalculation.text = String.format(Locale.getDefault(), "%.0f kg → %.0f kg", currentKg, newKg)

        validateAndCapThreshold()
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val showPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                    binding.etAddDate.setText(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etAddDate.setOnClickListener { showPicker() }
        binding.tilAddDate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupSaveButton() {
        binding.btnSaveAddStock.setOnClickListener {
            val isFr = LanguageManager.isFrench(requireContext())
            val maxThreshold = getMaxThresholdKg()
            if (thresholdKg > maxThreshold) {
                val formattedMax = if (maxThreshold % 1.0 == 0.0) {
                    String.format(Locale.getDefault(), "%.0f", maxThreshold)
                } else {
                    String.format(Locale.getDefault(), "%.1f", maxThreshold)
                }
                val msg = if (isFr) "Le seuil d'alerte ne peut pas dépasser 90% du stock disponible ($formattedMax kg max)" else "Alert threshold cannot exceed 90% of available stock ($formattedMax kg max)"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            viewModel.addOrUpdateFeedStock(selectedFeedName, addQtyKg, thresholdKg)
            val toastMsg = if (isFr) "Stock ajouté (+$addQtyKg kg)" else "Stock added (+$addQtyKg kg)"
            Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
