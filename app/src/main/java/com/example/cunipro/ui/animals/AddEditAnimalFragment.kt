package com.example.cunipro.ui.animals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.databinding.FragmentAddEditAnimalBinding
import java.util.Calendar
import java.util.Locale

class AddEditAnimalFragment : Fragment() {

    private var _binding: FragmentAddEditAnimalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimalsViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        AnimalsViewModelFactory(app.animalRepository)
    }

    private var animalId: Long = -1L
    private var currentAnimal: AnimalEntity? = null
    private var selectedSex: String = "F" // Default "F" (Femelle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getLong("animalId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupDropdowns()
        setupSexToggle()
        setupDatePicker()
        setupSaveButton()

        if (animalId != -1L) {
            binding.tvHeaderTitle.text = getString(R.string.animal_edit_title)
            binding.tvTagProposedBadge.visibility = View.GONE
            observeAnimalToEdit()
        } else {
            binding.tvHeaderTitle.text = getString(R.string.animal_add_title)
            binding.tvTagProposedBadge.visibility = View.VISIBLE
            loadSuggestedTagNumber()
        }
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadSuggestedTagNumber() {
        viewModel.getSuggestedTagNumber { suggested ->
            if (_binding != null && binding.etTagNumber.text.isNullOrEmpty()) {
                binding.etTagNumber.setText(suggested)
            }
        }
    }

    private fun observeAnimalToEdit() {
        viewModel.getAnimal(animalId).observe(viewLifecycleOwner) { animal ->
            if (animal != null) {
                currentAnimal = animal
                binding.etTagNumber.setText(animal.tagNumber)

                val isMale = animal.sex.equals("M", ignoreCase = true) || animal.sex.equals("Mâle", ignoreCase = true) || animal.sex.equals("Male", ignoreCase = true)
                updateSexSelection(if (isMale) "M" else "F")

                binding.actvBreed.setText(animal.breed ?: "", false)
                binding.etBirthDate.setText(animal.birthDate ?: "")
                binding.etWeight.setText(animal.weightGrams?.let { String.format(Locale.US, "%.1f", it / 1000.0) } ?: "")
                binding.actvOrigin.setText(animal.origin ?: "", false)

                when (animal.category.uppercase(Locale.getDefault())) {
                    "PRIMIPARE" -> {
                        binding.chipPrimipare.visibility = View.VISIBLE
                        binding.chipMultipare.visibility = View.VISIBLE
                        binding.chipPrimipare.isChecked = true
                    }
                    "MULTIPARE" -> {
                        binding.chipPrimipare.visibility = View.VISIBLE
                        binding.chipMultipare.visibility = View.VISIBLE
                        binding.chipMultipare.isChecked = true
                    }
                    "ENGRAISSEMENT" -> binding.chipEngraissement.isChecked = true
                    "SEVRE", "SÉVRÉS" -> binding.chipSevre.isChecked = true
                    "VENDU" -> binding.chipVendu.isChecked = true
                    "DECEDE", "DÉCÉDÉ" -> binding.chipDecede.isChecked = true
                    else -> binding.chipReproducteur.isChecked = true
                }

                binding.etNotes.setText(animal.notes ?: "")
            }
        }
    }

    private fun setupDropdowns() {
        val breeds = arrayOf(
            getString(R.string.breed_nz_white),
            getString(R.string.breed_fauve_bourgogne),
            getString(R.string.breed_giant_flanders),
            getString(R.string.breed_chinchilla),
            getString(R.string.breed_french_papillon),
            getString(R.string.breed_californian),
            getString(R.string.breed_local)
        )
        val breedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, breeds)
        binding.actvBreed.setAdapter(breedAdapter)

        val origins = arrayOf(
            getString(R.string.origin_born_on_farm),
            getString(R.string.origin_purchase),
            getString(R.string.origin_donation),
            getString(R.string.origin_exchange)
        )
        val originAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, origins)
        binding.actvOrigin.setAdapter(originAdapter)
    }

    private fun setupSexToggle() {
        updateSexSelection("F") // Default Femelle

        binding.btnSexMale.setOnClickListener {
            updateSexSelection("M")
        }

        binding.btnSexFemale.setOnClickListener {
            updateSexSelection("F")
        }
    }

    private fun updateSexSelection(sex: String) {
        selectedSex = sex
        val greenPrimary = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary)
        val greenLight = ContextCompat.getColor(requireContext(), R.color.cuni_green_light)
        val greenContainer = ContextCompat.getColor(requireContext(), R.color.cuni_green_container)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.cuni_text_primary)

        if (sex == "M") {
            // Male selected: hide female-specific categories (Primipare, Multipare)
            binding.btnSexMale.apply {
                setBackgroundColor(greenLight)
                setTextColor(greenPrimary)
                strokeColor = android.content.res.ColorStateList.valueOf(greenPrimary)
                strokeWidth = (2 * resources.displayMetrics.density).toInt()
            }
            binding.btnSexFemale.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textPrimary)
                strokeColor = android.content.res.ColorStateList.valueOf(greenContainer)
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
            }

            if (binding.chipPrimipare.isChecked || binding.chipMultipare.isChecked) {
                binding.chipReproducteur.isChecked = true
            }
            binding.chipPrimipare.visibility = View.GONE
            binding.chipMultipare.visibility = View.GONE
        } else {
            // Female selected: show female-specific categories (Primipare, Multipare)
            binding.btnSexFemale.apply {
                setBackgroundColor(greenLight)
                setTextColor(greenPrimary)
                strokeColor = android.content.res.ColorStateList.valueOf(greenPrimary)
                strokeWidth = (2 * resources.displayMetrics.density).toInt()
            }
            binding.btnSexMale.apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textPrimary)
                strokeColor = android.content.res.ColorStateList.valueOf(greenContainer)
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
            }

            binding.chipPrimipare.visibility = View.VISIBLE
            binding.chipMultipare.visibility = View.VISIBLE
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val showPicker = {
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    binding.etBirthDate.setText(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.etBirthDate.setOnClickListener { showPicker() }
        binding.tilBirthDate.setEndIconOnClickListener { showPicker() }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val tagNumber = binding.etTagNumber.text.toString().trim()
            if (tagNumber.isEmpty()) {
                binding.tilTagNumber.error = getString(R.string.animal_tag)
                return@setOnClickListener
            } else {
                binding.tilTagNumber.error = null
            }

            val breed = binding.actvBreed.text.toString().trim().ifEmpty { null }
            val birthDate = binding.etBirthDate.text.toString().trim().ifEmpty { null }
            val weightKg = binding.etWeight.text.toString().trim().toDoubleOrNull()
            val weightGrams = weightKg?.let { (it * 1000).toInt() }
            val origin = binding.actvOrigin.text.toString().trim().ifEmpty { null }

            val category = when (binding.chipGroupCategory.checkedChipId) {
                R.id.chipPrimipare -> "PRIMIPARE"
                R.id.chipMultipare -> "MULTIPARE"
                R.id.chipEngraissement -> "ENGRAISSEMENT"
                R.id.chipSevre -> "SEVRE"
                R.id.chipVendu -> "VENDU"
                R.id.chipDecede -> "DECEDE"
                else -> "REPRODUCTEUR"
            }

            val notes = binding.etNotes.text.toString().trim().ifEmpty { null }

            val status = when (category) {
                "DECEDE" -> "DECEDE"
                "VENDU" -> "VENDU"
                else -> "VIVANT"
            }

            if (animalId == -1L || currentAnimal == null) {
                viewModel.addAnimal(
                    tagNumber = tagNumber,
                    sex = selectedSex,
                    breed = breed,
                    birthDate = birthDate,
                    weightGrams = weightGrams,
                    origin = origin,
                    category = category,
                    notes = notes,
                    status = status
                )
                Toast.makeText(requireContext(), "$tagNumber", Toast.LENGTH_SHORT).show()
            } else {
                val updated = currentAnimal!!.copy(
                    tagNumber = tagNumber,
                    sex = selectedSex,
                    breed = breed,
                    birthDate = birthDate,
                    weightGrams = weightGrams,
                    origin = origin,
                    category = category,
                    status = status,
                    notes = notes
                )
                viewModel.updateAnimal(updated)
                Toast.makeText(requireContext(), "$tagNumber", Toast.LENGTH_SHORT).show()
            }

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
