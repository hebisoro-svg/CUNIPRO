package com.example.cunipro.ui.reproduction

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.data.local.entity.ReproductionEntity
import com.example.cunipro.databinding.FragmentRecordKindlingBinding
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.setupAutoRepeat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecordKindlingFragment : Fragment() {

    private var _binding: FragmentRecordKindlingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReproductionViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        ReproductionViewModelFactory(app.reproductionRepository)
    }

    private var reproductionId: Long = -1L
    private var currentReproduction: ReproductionEntity? = null

    private var aliveCount: Int = 0
    private var deadCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reproductionId = arguments?.getLong("reproductionId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordKindlingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackKindling.setOnClickListener {
            findNavController().navigateUp()
        }

        setupSteppers()
        setupDatePicker()
        setupDateWatcher()

        val todayFormatted = formatDateInput(Calendar.getInstance().time)
        binding.etKindlingDate.setText(todayFormatted)
        updateCalculatedWeaningDate(todayFormatted)

        if (reproductionId != -1L) {
            loadReproduction()
        } else {
            Toast.makeText(requireContext(), "Données non trouvées", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        setupSaveButton()
    }

    private fun loadReproduction() {
        viewModel.allReproductions.observe(viewLifecycleOwner) { list ->
            val repro = list.find { it.id == reproductionId }
            if (repro != null) {
                currentReproduction = repro
                binding.tvKindlingCoupleTags.text = "${repro.femaleTag} × ${repro.maleTag ?: "Inconnu"}"

                val matingDateDisp = formatDisplayDate(repro.matingDate)
                val expectedKindlingDisp = formatDisplayDate(repro.expectedKindlingDate ?: repro.matingDate)
                binding.tvKindlingMatingDates.text = "Saillie du $matingDateDisp · mise bas prévue le $expectedKindlingDisp"

                if (repro.bornAliveCount > 0) aliveCount = repro.bornAliveCount
                if (repro.bornDeadCount > 0) deadCount = repro.bornDeadCount
                updateCounts()
            }
        }
    }

    private fun setupSteppers() {
        updateCounts()

        binding.btnMinusAlive.setupAutoRepeat {
            if (aliveCount > 0) {
                aliveCount--
                updateCounts()
            }
        }

        binding.btnPlusAlive.setupAutoRepeat {
            aliveCount++
            updateCounts()
        }

        binding.btnMinusDead.setupAutoRepeat {
            if (deadCount > 0) {
                deadCount--
                updateCounts()
            }
        }

        binding.btnPlusDead.setupAutoRepeat {
            deadCount++
            updateCounts()
        }
    }

    private fun updateCounts() {
        if (_binding == null) return
        binding.tvAliveCount.text = aliveCount.toString()
        binding.tvDeadCount.text = deadCount.toString()
        val total = aliveCount + deadCount
        binding.tvTotalLitterCount.text = "$total lapereaux"
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val showPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    val formatted = formatDateInput(cal.time)
                    binding.etKindlingDate.setText(formatted)
                    updateCalculatedWeaningDate(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etKindlingDate.setOnClickListener { showPicker() }
        binding.tilKindlingDate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupDateWatcher() {
        binding.etKindlingDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCalculatedWeaningDate(s?.toString()?.trim() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateCalculatedWeaningDate(kindlingDateInput: String) {
        if (kindlingDateInput.isBlank()) return
        val sevrageDays = BreedingDateCalculator.getSevrageDays(requireContext())
        val isoKindling = parseInputToIso(kindlingDateInput)
        val isoWeaning = BreedingDateCalculator.calculateExpectedWeaningDate(isoKindling, sevrageDays)
        val displayWeaning = formatDisplayDate(isoWeaning)
        binding.tvCalculatedKindlingWeaning.text = "📅 Sevrage prévu le $displayWeaning"
    }

    private fun setupSaveButton() {
        binding.btnSaveKindling.setOnClickListener {
            val repro = currentReproduction
            if (repro == null) {
                Toast.makeText(requireContext(), "Erreur", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateInput = binding.etKindlingDate.text.toString().trim()
            val isoKindlingDate = parseInputToIso(dateInput)

            viewModel.recordKindling(
                reproduction = repro,
                kindlingDate = isoKindlingDate,
                bornAlive = aliveCount,
                bornDead = deadCount
            )

            // Déclencher le WorkManager pour mettre à jour les rappels de notification
            val work = androidx.work.OneTimeWorkRequestBuilder<com.example.cunipro.worker.ReproductionNotificationWorker>().build()
            androidx.work.WorkManager.getInstance(requireContext()).enqueue(work)

            Toast.makeText(
                requireContext(),
                "Mise bas enregistrée !",
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
