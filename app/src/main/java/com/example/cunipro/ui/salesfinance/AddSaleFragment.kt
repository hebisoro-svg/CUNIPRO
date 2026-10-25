package com.example.cunipro.ui.salesfinance

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.databinding.FragmentAddSaleBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddSaleFragment : Fragment() {

    private var _binding: FragmentAddSaleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SalesFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        SalesFinanceViewModelFactory(app.financeRepository, app.animalRepository)
    }

    private var selectedType = "LAPINS" // "LAPINS", "ALIMENTS", "MATERIEL"
    private var quantity = 1
    private var availableAnimalsList: List<AnimalEntity> = emptyList()
    private var selectedAnimalId: Long? = null

    private var selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddSaleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupTypeSelector()
        setupDatePicker()
        observeAnimals()
        setupSubmitButton()

        selectSaleType("LAPINS")
    }

    private fun setupTypeSelector() {
        binding.btnTypeRabbit.setOnClickListener { selectSaleType("LAPINS") }
        binding.btnTypeFeed.setOnClickListener { selectSaleType("ALIMENTS") }
        binding.btnTypeEquipment.setOnClickListener { selectSaleType("MATERIEL") }
    }

    private fun selectSaleType(type: String) {
        selectedType = type
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

        setButtonSelected(binding.btnTypeRabbit, type == "LAPINS")
        setButtonSelected(binding.btnTypeFeed, type == "ALIMENTS")
        setButtonSelected(binding.btnTypeEquipment, type == "MATERIEL")

        when (type) {
            "LAPINS" -> {
                binding.layoutRabbitSelector.visibility = View.VISIBLE
                binding.layoutItemNameInput.visibility = View.GONE
                binding.tvSaleInfoText.text = getString(R.string.sale_rabbit_info)
            }
            "ALIMENTS" -> {
                binding.layoutRabbitSelector.visibility = View.GONE
                binding.layoutItemNameInput.visibility = View.VISIBLE
                binding.tilItemName.hint = getString(R.string.expense_feed_hint)
                binding.etItemName.hint = null
                binding.tvSaleInfoText.text = getString(R.string.sale_feed_info)
            }
            "MATERIEL" -> {
                binding.layoutRabbitSelector.visibility = View.GONE
                binding.layoutItemNameInput.visibility = View.VISIBLE
                binding.tilItemName.hint = getString(R.string.expense_other_hint)
                binding.etItemName.hint = null
                binding.tvSaleInfoText.text = getString(R.string.sale_equipment_info)
            }
        }
    }

    private fun setupDatePicker() {
        binding.etSaleDate.setText(BreedingDateCalculator.formatDateDisplay(selectedDateFormatted))

        binding.etSaleDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.time)
                    binding.etSaleDate.setText(BreedingDateCalculator.formatDateDisplay(selectedDateFormatted))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun observeAnimals() {
        viewModel.availableAnimals.observe(viewLifecycleOwner) { animals ->
            availableAnimalsList = animals
            val adapterItems = animals.map { "${it.tagNumber} · ${it.breed ?: "Local"}" }
            val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, adapterItems)
            binding.spinnerAnimal.setAdapter(arrayAdapter)

            binding.spinnerAnimal.setOnItemClickListener { _, _, position, _ ->
                if (position in animals.indices) {
                    selectedAnimalId = animals[position].id
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitSale.setOnClickListener {
            val isFr = LanguageManager.isFrench(requireContext())
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

            val clientName = binding.etClientName.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

            data class SaleData(
                val category: String,
                val description: String,
                val itemDetails: String,
                val animalId: Long?,
                val animalTag: String?
            )

            val saleData = when (selectedType) {
                "LAPINS" -> {
                    val rawInput = binding.spinnerAnimal.text.toString().trim()
                    val tagPart = rawInput.substringBefore("·").trim()
                    val foundAnimal = availableAnimalsList.find {
                        it.id == selectedAnimalId ||
                        it.tagNumber.equals(rawInput, ignoreCase = true) ||
                        it.tagNumber.equals(tagPart, ignoreCase = true)
                    }
                    val animalId = foundAnimal?.id ?: selectedAnimalId
                    val rabbitTag = foundAnimal?.tagNumber ?: tagPart.ifBlank { "Lapin" }
                    val desc = if (isFr) "Vente de lapin ($rabbitTag)" else "Rabbit sale ($rabbitTag)"
                    val details = "$quantity $rabbitTag"
                    SaleData("VENTE_LAPIN", desc, details, animalId, rabbitTag)
                }
                "ALIMENTS" -> {
                    val itemName = binding.etItemName.text?.toString()?.trim().orEmpty().ifBlank { "Granulés" }
                    val desc = if (isFr) "Vente d'aliments" else "Feed sale"
                    val details = "$quantity $itemName"
                    SaleData("VENTE_ALIMENT", desc, details, null, null)
                }
                else -> {
                    val itemName = binding.etItemName.text?.toString()?.trim().orEmpty().ifBlank { "Matériel" }
                    val desc = if (isFr) "Vente de matériel" else "Equipment sale"
                    val details = "$quantity $itemName"
                    SaleData("VENTE_MATERIEL", desc, details, null, null)
                }
            }

            viewModel.recordSale(
                category = saleData.category,
                amount = amount,
                date = selectedDateFormatted,
                description = saleData.description,
                clientName = clientName,
                itemDetails = saleData.itemDetails,
                quantity = quantity,
                selectedAnimalId = saleData.animalId,
                selectedAnimalTag = saleData.animalTag
            )

            Toast.makeText(requireContext(), if (isFr) "Vente enregistrée !" else "Sale recorded!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
