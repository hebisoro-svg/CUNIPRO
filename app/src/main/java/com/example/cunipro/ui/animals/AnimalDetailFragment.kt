package com.example.cunipro.ui.animals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.databinding.FragmentAnimalDetailBinding
import com.example.cunipro.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnimalDetailFragment : Fragment() {

    private var _binding: FragmentAnimalDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimalsViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        AnimalsViewModelFactory(app.animalRepository)
    }

    private var animalId: Long = -1L
    private var currentAnimal: AnimalEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getLong("animalId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackDetail.setOnClickListener {
            findNavController().navigateUp()
        }

        if (animalId != -1L) {
            observeAnimal()
        } else {
            val isFr = LanguageManager.isFrench(requireContext())
            Toast.makeText(requireContext(), if (isFr) "Lapin non trouvé" else "Rabbit not found", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun observeAnimal() {
        viewModel.getAnimal(animalId).observe(viewLifecycleOwner) { animal ->
            if (animal == null) return@observe
            currentAnimal = animal
            val isFr = LanguageManager.isFrench(requireContext())

            binding.tvDetailTagNumber.text = animal.tagNumber
            binding.tvDetailTag.text = animal.tagNumber

            val isMale = animal.sex.equals("M", ignoreCase = true) || animal.sex.equals("Mâle", ignoreCase = true) || animal.sex.equals("Male", ignoreCase = true)
            val sexDisplay = if (isMale) (if (isFr) "Mâle" else "Male") else (if (isFr) "Femelle" else "Female")
            binding.tvHeaderSexBadge.text = sexDisplay
            binding.tvDetailSex.text = sexDisplay

            val categoryDisplay = formatCategory(animal.category, isFr)
            binding.tvHeaderCategoryBadge.text = categoryDisplay
            binding.tvDetailStatusBadge.text = categoryDisplay

            binding.tvDetailBreed.text = animal.breed.takeIf { !it.isNullOrBlank() } ?: (if (isFr) "Local" else "Local")

            binding.tvDetailBirthDate.text = formatBirthDateWithAge(animal.birthDate, isFr)

            val weightStr = if (animal.weightGrams != null && animal.weightGrams > 0) {
                val kg = animal.weightGrams / 1000.0
                String.format(Locale.getDefault(), "%.1f kg", kg)
            } else {
                "- kg"
            }
            binding.tvDetailWeight.text = weightStr

            binding.tvDetailOrigin.text = animal.origin.takeIf { !it.isNullOrBlank() } ?: (if (isFr) "Élevage (Né sur place)" else "Farm (Born on site)")

            binding.tvDetailNotes.text = animal.notes.takeIf { !it.isNullOrBlank() } ?: (if (isFr) "Aucune observation enregistrée." else "No notes recorded.")

            val openEdit = {
                val bundle = Bundle().apply {
                    putLong("animalId", animal.id)
                }
                findNavController().navigate(R.id.action_animalDetailFragment_to_addEditAnimalFragment, bundle)
            }

            binding.btnHeaderEdit.setOnClickListener { openEdit() }
            binding.btnEditDetail.setOnClickListener { openEdit() }

            binding.btnDeleteDetail.setOnClickListener {
                showDeleteConfirmation(animal)
            }
        }
    }

    private fun formatCategory(category: String, isFr: Boolean): String {
        return when (category.uppercase(Locale.getDefault())) {
            "ENGRAISSEMENT" -> if (isFr) "Engraissement" else "Fattening"
            "SEVRE", "SÉVRÉS" -> if (isFr) "Sevré" else "Weaned"
            "VENDU" -> if (isFr) "Vendu" else "Sold"
            "DECEDE", "DÉCÉDÉ" -> if (isFr) "Décédé" else "Deceased"
            "PRIMIPARE" -> if (isFr) "Primipare" else "Primiparous"
            "MULTIPARE" -> if (isFr) "Multipare" else "Multiparous"
            else -> if (isFr) "Reproducteur" else "Breeder"
        }
    }

    private fun formatBirthDateWithAge(birthDateStr: String?, isFr: Boolean): String {
        if (birthDateStr.isNullOrBlank()) return if (isFr) "Date inconnue" else "Unknown date"

        return try {
            val inputFormats = arrayOf(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
            )

            var date: java.util.Date? = null
            for (format in inputFormats) {
                try {
                    date = format.parse(birthDateStr)
                    if (date != null) break
                } catch (_: Exception) {}
            }

            if (date == null) return birthDateStr

            val birthCal = Calendar.getInstance().apply { time = date }
            val nowCal = Calendar.getInstance()

            var months = (nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12 +
                    (nowCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH))

            if (nowCal.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) {
                months--
            }

            val displayFormat = SimpleDateFormat("d MMM yyyy", if (isFr) Locale.FRANCE else Locale.ENGLISH)
            val dateFormatted = displayFormat.format(date)

            val ageStr = when {
                months <= 0 -> if (isFr) "Moins d'un mois" else "Under 1 month"
                months < 12 -> if (isFr) "$months mois" else "$months months"
                else -> {
                    val years = months / 12
                    val remMonths = months % 12
                    if (remMonths > 0) {
                        if (isFr) "$years an(s) $remMonths mois" else "$years yr(s) $remMonths mo"
                    } else {
                        if (isFr) "$years an(s)" else "$years yr(s)"
                    }
                }
            }

            "$dateFormatted · $ageStr"
        } catch (_: Exception) {
            birthDateStr
        }
    }

    private fun showDeleteConfirmation(animal: AnimalEntity) {
        val isFr = LanguageManager.isFrench(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.animal_delete_confirm_title))
            .setMessage(if (isFr) "Voulez-vous vraiment supprimer le lapin ${animal.tagNumber} ?" else "Are you sure you want to delete rabbit ${animal.tagNumber}?")
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                viewModel.deleteAnimal(animal)
                Toast.makeText(requireContext(), if (isFr) "Lapin supprimé" else "Rabbit deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
