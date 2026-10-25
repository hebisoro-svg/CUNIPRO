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
import com.example.cunipro.databinding.FragmentAddMortalityBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import java.util.Calendar

class AddMortalityFragment : Fragment() {

    private var _binding: FragmentAddMortalityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthFinanceViewModel by activityViewModels {
        val app = requireActivity().application as CuniProApplication
        HealthFinanceViewModelFactory(app.healthRepository, app.financeRepository, app.animalRepository)
    }

    private var livingAnimals: List<AnimalEntity> = emptyList()
    private var deceasedAnimalIds: Set<Long> = emptySet()
    private var deceasedAnimalTags: Set<String> = emptySet()
    private val deathCount = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMortalityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackMortality.setOnClickListener {
            findNavController().navigateUp()
        }

        setupMortalityForm()
    }

    private fun setupMortalityForm() {
        var cachedAnimals = emptyList<AnimalEntity>()
        var cachedMortalities = emptyList<HealthLogEntity>()

        fun updateDropdown() {
            if (_binding == null) return
            deceasedAnimalIds = cachedMortalities.mapNotNull { it.animalId }.toSet()
            deceasedAnimalTags = cachedMortalities.mapNotNull {
                it.animalTag?.split("·", "(")?.firstOrNull()?.trim()?.lowercase()
            }.toSet()

            livingAnimals = cachedAnimals.filter { animal ->
                animal.isLiving() &&
                !deceasedAnimalIds.contains(animal.id) &&
                !deceasedAnimalTags.contains(animal.tagNumber.trim().lowercase())
            }

            val noLivingStr = getString(R.string.mortality_no_living)
            val animalOptions = mutableListOf<String>()
            animalOptions.addAll(livingAnimals.map { "${it.tagNumber} (${it.breed ?: "Local"})" })
            if (animalOptions.isEmpty()) {
                animalOptions.add(noLivingStr)
            }
            val animalAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, animalOptions)
            binding.actvMortalityAnimalSelect.setAdapter(animalAdapter)

            val currentText = binding.actvMortalityAnimalSelect.text.toString()
            if (currentText.isEmpty() || currentText !in animalOptions) {
                binding.actvMortalityAnimalSelect.setText(animalOptions.first(), false)
            }
        }

        viewModel.mortalityLogs.observe(viewLifecycleOwner) { mortalities ->
            cachedMortalities = mortalities
            val currentTotalDeaths = mortalities.sumOf { it.deathCount }
            updateSummaryUI(currentTotalDeaths)
            updateDropdown()
        }

        val causes = listOf(
            getString(R.string.cause_disease),
            getString(R.string.cause_stillborn),
            getString(R.string.cause_heat),
            getString(R.string.cause_accident),
            getString(R.string.cause_unknown)
        )
        val causeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, causes)
        binding.actvMortalityCause.setAdapter(causeAdapter)
        binding.actvMortalityCause.setText(getString(R.string.cause_disease), false)

        val todayStr = BreedingDateCalculator.getTodayString()
        binding.etMortalityDate.setText(todayStr)

        binding.etMortalityDate.setOnClickListener {
            showDatePicker { selectedDate -> binding.etMortalityDate.setText(selectedDate) }
        }

        viewModel.allAnimals.observe(viewLifecycleOwner) { animals ->
            cachedAnimals = animals
            updateDropdown()
        }

        binding.btnSubmitMortality.setOnClickListener {
            val selectedAnimalText = binding.actvMortalityAnimalSelect.text.toString().trim()
            val noLivingStr = getString(R.string.mortality_no_living)
            if (selectedAnimalText.isBlank() || selectedAnimalText == noLivingStr || selectedAnimalText == "Aucun lapin vivant" || livingAnimals.isEmpty()) {
                val isFr = LanguageManager.isFrench(requireContext())
                Toast.makeText(requireContext(), if (isFr) "Veuillez sélectionner un lapin vivant valide." else "Please select a valid living rabbit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val foundAnimal = livingAnimals.find { "${it.tagNumber} (${it.breed ?: "Local"})" == selectedAnimalText }
            if (foundAnimal == null) {
                val isFr = LanguageManager.isFrench(requireContext())
                Toast.makeText(requireContext(), if (isFr) "Ce lapin n'est pas disponible !" else "This rabbit is not available!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (deceasedAnimalIds.contains(foundAnimal.id) || deceasedAnimalTags.contains(foundAnimal.tagNumber.trim().lowercase())) {
                val isFr = LanguageManager.isFrench(requireContext())
                Toast.makeText(requireContext(), if (isFr) "Ce lapin (${foundAnimal.tagNumber}) a déjà été déclaré décédé !" else "Rabbit (${foundAnimal.tagNumber}) already declared deceased!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val selectedAnimalId = foundAnimal.id
            val animalTag = "${foundAnimal.tagNumber} · ${foundAnimal.breed ?: ""}".trimEnd(' ', '·')

            val deathDate = binding.etMortalityDate.text.toString().ifBlank { todayStr }
            val cause = binding.actvMortalityCause.text.toString()
            val notes = binding.etMortalityNotes.text.toString().trim()

            val healthLog = HealthLogEntity(
                animalId = selectedAnimalId,
                animalTag = animalTag,
                diseaseName = "Décès: $cause",
                treatmentType = "MALADIE",
                treatmentName = "Déclaration de décès",
                status = "TERMINE",
                treatmentDate = deathDate,
                isMortality = true,
                deathCount = deathCount,
                deathCause = cause,
                notes = notes
            )

            viewModel.addHealthLog(healthLog)
            val isFr = LanguageManager.isFrench(requireContext())
            Toast.makeText(requireContext(), if (isFr) "Décès déclaré pour ${foundAnimal.tagNumber}" else "Death recorded for ${foundAnimal.tagNumber}", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun updateSummaryUI(currentTotalDeaths: Int) {
        if (_binding == null) return
        val isFr = LanguageManager.isFrench(requireContext())
        binding.tvMortalityCount.text = if (isFr) "1 décès" else "1 death"
        binding.tvMortalityImpactCount.text = if (isFr) "Cumul après enregistrement : ${currentTotalDeaths + 1} décès" else "Total after recording: ${currentTotalDeaths + 1} deaths"
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
