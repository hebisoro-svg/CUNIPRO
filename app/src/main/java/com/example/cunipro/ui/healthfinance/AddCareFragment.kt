package com.example.cunipro.ui.healthfinance

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.data.local.entity.HealthLogEntity
import com.example.cunipro.databinding.FragmentAddCareBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import java.util.Calendar

class AddCareFragment : Fragment() {

    private var _binding: FragmentAddCareBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        HealthFinanceViewModelFactory(app.healthRepository, app.financeRepository, app.animalRepository)
    }

    private var livingAnimals: List<AnimalEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackCare.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCareForm()
    }

    private fun setupCareForm() {
        val diseases = listOf(
            getString(R.string.disease_coccidiosis),
            getString(R.string.disease_ear_mange),
            getString(R.string.disease_myxomatosis),
            getString(R.string.disease_diarrhea),
            getString(R.string.disease_coryza),
            getString(R.string.disease_other)
        )
        val diseaseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, diseases)
        binding.actvCareDisease.setAdapter(diseaseAdapter)
        binding.actvCareDisease.setText(getString(R.string.disease_coccidiosis), false)

        val todayStr = BreedingDateCalculator.getTodayString()
        binding.etCareDate.setText(todayStr)

        val defaultNextDate = BreedingDateCalculator.addDaysToDate(todayStr, 7)
        binding.etCareNextDueDate.setText(defaultNextDate)

        fun updateReminderVisibility(isReminderEnabled: Boolean) {
            binding.tilCareNextDueDate.visibility = if (isReminderEnabled) View.VISIBLE else View.GONE
        }

        updateReminderVisibility(binding.switchCareReminder.isChecked)

        binding.switchCareReminder.setOnCheckedChangeListener { _, isChecked ->
            updateReminderVisibility(isChecked)
        }

        binding.etCareDate.setOnClickListener {
            showDatePicker { selectedDate -> binding.etCareDate.setText(selectedDate) }
        }

        binding.etCareNextDueDate.setOnClickListener {
            showDatePicker { selectedDate -> binding.etCareNextDueDate.setText(selectedDate) }
        }

        var cachedAnimals = emptyList<AnimalEntity>()
        var cachedMortalities = emptyList<HealthLogEntity>()

        fun updateCareDropdown() {
            if (_binding == null) return
            val deceasedIds = cachedMortalities.mapNotNull { it.animalId }.toSet()
            val deceasedTags = cachedMortalities.mapNotNull {
                it.animalTag?.split("·", "(")?.firstOrNull()?.trim()?.lowercase()
            }.toSet()

            livingAnimals = cachedAnimals.filter { animal ->
                animal.isLiving() &&
                !deceasedIds.contains(animal.id) &&
                !deceasedTags.contains(animal.tagNumber.trim().lowercase())
            }

            val entireHerdStr = getString(R.string.care_entire_farm)
            val animalOptions = mutableListOf(entireHerdStr)
            animalOptions.addAll(livingAnimals.map { "${it.tagNumber} (${it.breed ?: "Local"})" })
            val animalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, animalOptions)
            binding.actvCareAnimalSelect.setAdapter(animalAdapter)
            if (binding.actvCareAnimalSelect.text.isEmpty()) {
                binding.actvCareAnimalSelect.setText(entireHerdStr, false)
            }
        }

        viewModel.mortalityLogs.observe(viewLifecycleOwner) { mortalities ->
            cachedMortalities = mortalities
            updateCareDropdown()
        }

        viewModel.allAnimals.observe(viewLifecycleOwner) { animals ->
            cachedAnimals = animals
            updateCareDropdown()
        }

        binding.btnSubmitCare.setOnClickListener {
            val selectedAnimalText = binding.actvCareAnimalSelect.text.toString()
            val entireHerdStr = getString(R.string.care_entire_farm)
            var selectedAnimalId: Long? = null
            var animalTag: String? = selectedAnimalText

            if (selectedAnimalText != entireHerdStr && selectedAnimalText != "Tout l'élevage" && selectedAnimalText != "Entire herd") {
                val foundAnimal = livingAnimals.find { "${it.tagNumber} (${it.breed ?: "Local"})" == selectedAnimalText }
                if (foundAnimal != null) {
                    selectedAnimalId = foundAnimal.id
                    animalTag = "${foundAnimal.tagNumber} · ${foundAnimal.breed ?: ""}".trimEnd(' ', '·')
                }
            }

            val diseaseName = binding.actvCareDisease.text.toString().ifBlank { getString(R.string.disease_coccidiosis) }
            val symptoms = binding.etCareSymptoms.text.toString().trim()
            val treatmentName = binding.etCareTreatmentName.text.toString().trim().ifBlank { "Traitement" }
            val dosage = binding.etCareDosage.text.toString().trim()
            val careDate = binding.etCareDate.text.toString().ifBlank { todayStr }
            val notes = binding.etCareNotes.text.toString().trim()

            val hasReminder = binding.switchCareReminder.isChecked
            val nextDueDate = if (hasReminder) binding.etCareNextDueDate.text.toString().ifBlank { null } else null

            val healthLog = HealthLogEntity(
                animalId = selectedAnimalId,
                animalTag = animalTag,
                diseaseName = diseaseName,
                symptoms = symptoms.ifBlank { null },
                treatmentType = "TRAITEMENT",
                treatmentName = treatmentName,
                dosage = dosage.ifBlank { null },
                status = "EN_COURS",
                treatmentDate = careDate,
                nextDueDate = nextDueDate,
                isMortality = false,
                notes = notes
            )

            viewModel.addHealthLog(healthLog)
            val isFr = LanguageManager.isFrench(requireContext())
            Toast.makeText(requireContext(), if (isFr) "Soin enregistré" else "Care recorded", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun AnimalEntity.isLiving(): Boolean {
        return !status.equals("DECEDE", ignoreCase = true) &&
               !status.equals("DÉCÉDÉ", ignoreCase = true) &&
               !status.equals("VENDU", ignoreCase = true) &&
               !category.contains("DECED", ignoreCase = true) &&
               !category.equals("VENDU", ignoreCase = true)
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
            onDateSelected(formatted)
        }, year, month, day).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
