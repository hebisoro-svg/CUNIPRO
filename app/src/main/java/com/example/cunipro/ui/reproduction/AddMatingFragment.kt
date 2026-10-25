package com.example.cunipro.ui.reproduction

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.databinding.FragmentAddMatingBinding
import com.example.cunipro.util.BreedingDateCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddMatingFragment : Fragment() {

    private var _binding: FragmentAddMatingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReproductionViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        ReproductionViewModelFactory(app.reproductionRepository)
    }

    private var femaleList: List<com.example.cunipro.data.local.entity.AnimalEntity> = emptyList()
    private var maleList: List<com.example.cunipro.data.local.entity.AnimalEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackMating.setOnClickListener {
            findNavController().navigateUp()
        }

        setupDatePicker()
        setupAnimalDropdowns()
        setupCalculatedDatesWatcher()
        setupSaveButton()

        val todayFormatted = formatDateInput(Calendar.getInstance().time)
        binding.etMatingDate.setText(todayFormatted)
        updateCalculatedDates(todayFormatted)
    }

    private fun setupAnimalDropdowns() {
        val app = requireActivity().application as CuniProApplication
        app.animalRepository.activeAnimals.asLiveData().observe(viewLifecycleOwner) { animals ->
            val livingAnimals = animals.filter { animal ->
                val isLivingStatus = animal.status.equals("VIVANT", ignoreCase = true) || animal.status.isBlank()
                val isNotDeceasedCategory = !animal.category.equals("DECEDE", ignoreCase = true) && !animal.category.equals("DÉCÉDÉ", ignoreCase = true)
                val isNotSoldCategory = !animal.category.equals("VENDU", ignoreCase = true)
                isLivingStatus && isNotDeceasedCategory && isNotSoldCategory
            }

            val females = livingAnimals.filter {
                it.sex.equals("F", ignoreCase = true) || it.sex.equals("Femelle", ignoreCase = true)
            }
            val males = livingAnimals.filter {
                it.sex.equals("M", ignoreCase = true) || it.sex.equals("Mâle", ignoreCase = true)
            }

            femaleList = females
            maleList = males

            val femaleOptions = females.map { "${it.tagNumber} · ${it.breed ?: "Local"}" }
            val femaleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, femaleOptions)
            binding.actvFemale.setAdapter(femaleAdapter)

            val maleOptions = males.map { "${it.tagNumber} · ${it.breed ?: "Local"}" }
            val maleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, maleOptions)
            binding.actvMale.setAdapter(maleAdapter)
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val showPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    val formatted = formatDateInput(cal.time)
                    binding.etMatingDate.setText(formatted)
                    updateCalculatedDates(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etMatingDate.setOnClickListener { showPicker() }
        binding.tilMatingDate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupCalculatedDatesWatcher() {
        binding.etMatingDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCalculatedDates(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateCalculatedDates(matingDateStr: String) {
        if (matingDateStr.isBlank()) return

        val palpationDays = BreedingDateCalculator.getPalpationDays(requireContext())
        val gestationDays = BreedingDateCalculator.getGestationDays(requireContext())
        val sevrageDays = BreedingDateCalculator.getSevrageDays(requireContext())

        val isoDate = parseInputToIso(matingDateStr)
        val palpationIso = BreedingDateCalculator.calculatePalpationDate(isoDate, palpationDays)
        val kindlingIso = BreedingDateCalculator.calculateExpectedKindlingDate(isoDate, gestationDays)
        val weaningIso = BreedingDateCalculator.calculateExpectedWeaningDate(kindlingIso, sevrageDays)

        binding.tvCalculatedPalpation.text = formatDisplayDate(palpationIso)
        binding.tvCalculatedKindling.text = formatDisplayDate(kindlingIso)
        binding.tvCalculatedWeaning.text = formatDisplayDate(weaningIso)
    }

    private fun setupSaveButton() {
        binding.btnSaveMating.setOnClickListener {
            val femaleText = binding.actvFemale.text.toString().trim()
            if (femaleText.isEmpty()) {
                binding.tilFemale.error = "Veuillez sélectionner la femelle"
                return@setOnClickListener
            } else {
                binding.tilFemale.error = null
            }

            val femaleTag = femaleText.split("·").firstOrNull()?.trim() ?: femaleText
            val maleText = binding.actvMale.text.toString().trim()
            val maleTag = if (maleText.isNotEmpty()) maleText.split("·").firstOrNull()?.trim() else null

            val matingDateInput = binding.etMatingDate.text.toString().trim()
            val isoMatingDate = parseInputToIso(matingDateInput)
            val notes = binding.etMatingNotes.text.toString().trim().ifEmpty { null }

            val femaleEntity = femaleList.find { it.tagNumber.equals(femaleTag, ignoreCase = true) }
            val maleEntity = maleList.find { it.tagNumber.equals(maleTag, ignoreCase = true) }

            viewModel.recordMating(
                femaleTag = femaleTag,
                maleTag = maleTag,
                matingDate = isoMatingDate,
                femaleId = femaleEntity?.id ?: 0L,
                maleId = maleEntity?.id,
                notes = notes
            )

            // Déclencher le WorkManager pour mettre à jour les rappels de notification
            val work = androidx.work.OneTimeWorkRequestBuilder<com.example.cunipro.worker.ReproductionNotificationWorker>().build()
            androidx.work.WorkManager.getInstance(requireContext()).enqueue(work)

            Toast.makeText(
                requireContext(),
                "Saillie enregistrée. Rappels créés !",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigateUp()
        }
    }

    private fun formatDateInput(date: java.util.Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(date)
    }

    private fun parseInputToIso(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val date = inputFormat.parse(dateStr)
            if (date != null) isoFormat.format(date) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    private fun formatDisplayDate(isoDateStr: String): String {
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
