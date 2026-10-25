package com.example.cunipro.ui.animals

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.local.entity.AnimalEntity
import com.example.cunipro.databinding.FragmentAnimalsBinding
import com.example.cunipro.ui.adapter.AnimalAdapter
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupTopActions
import java.util.Locale

class AnimalsFragment : Fragment() {

    private var _binding: FragmentAnimalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnimalsViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        AnimalsViewModelFactory(app.animalRepository)
    }

    private lateinit var animalAdapter: AnimalAdapter
    private var allAnimalsList: List<AnimalEntity> = emptyList()
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopActions(
            binding.includeTopActions.btnTopFinance,
            binding.includeTopActions.btnTopNotifications,
            binding.includeTopActions.btnTopSettings,
            binding.includeTopActions.tvNotificationBadge
        )

        setupRecyclerView()
        setupSearchAndFilters()
        observeData()

        binding.fabAddAnimal.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("animalId", -1L)
            }
            findNavController().navigate(
                R.id.action_animalsFragment_to_addEditAnimalFragment,
                bundle
            )
        }
    }

    private fun setupRecyclerView() {
        animalAdapter = AnimalAdapter(
            onItemClick = { animal ->
                val bundle = Bundle().apply {
                    putLong("animalId", animal.id)
                }
                findNavController().navigate(
                    R.id.action_animalsFragment_to_animalDetailFragment,
                    bundle
                )
            }
        )
        binding.rvAnimals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = animalAdapter
        }
    }

    private fun setupSearchAndFilters() {
        binding.etSearchAnimal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                filterAnimals()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, _ ->
            filterAnimals()
        }
    }

    private fun observeData() {
        viewModel.allAnimals.observe(viewLifecycleOwner) { animals ->
            allAnimalsList = animals
            updateCategoryChipCounts()
            filterAnimals()
        }
    }

    private fun updateCategoryChipCounts() {
        if (_binding == null) return

        val context = context ?: return
        val isFr = LanguageManager.isFrench(context)

        val total = allAnimalsList.size
        val reproCount = allAnimalsList.count {
            val cat = it.category.uppercase(Locale.getDefault())
            cat == "REPRODUCTEUR" || cat == "PRIMIPARE" || cat == "MULTIPARE"
        }
        val engraissCount = allAnimalsList.count { it.category.equals("ENGRAISSEMENT", ignoreCase = true) }
        val sevreCount = allAnimalsList.count { it.category.equals("SEVRE", ignoreCase = true) || it.category.equals("SÉVRÉS", ignoreCase = true) }
        val venduCount = allAnimalsList.count { it.category.equals("VENDU", ignoreCase = true) }
        val decedeCount = allAnimalsList.count { it.category.equals("DECEDE", ignoreCase = true) || it.category.equals("DÉCÉDÉ", ignoreCase = true) }

        binding.tvTotalAnimalsSubtitle.text = if (isFr) "$total lapins dans l'élevage" else "$total rabbits in herd"

        binding.chipAll.text = if (isFr) "Tous · $total" else "All · $total"
        binding.chipReproducteur.text = if (isFr) "Reproducteur · $reproCount" else "Breeders · $reproCount"
        binding.chipEngraissement.text = if (isFr) "Engraissement · $engraissCount" else "Fattening · $engraissCount"
        binding.chipSevre.text = if (isFr) "Sevré · $sevreCount" else "Weaned · $sevreCount"
        binding.chipVendu.text = if (isFr) "Vendu · $venduCount" else "Sold · $venduCount"
        binding.chipDecede.text = if (isFr) "Décédé · $decedeCount" else "Deceased · $decedeCount"
    }

    private fun filterAnimals() {
        if (_binding == null) return

        val checkedChipId = binding.chipGroupCategory.checkedChipId

        var list = allAnimalsList

        // 1. Filter by category chip
        list = when (checkedChipId) {
            R.id.chipReproducteur -> list.filter {
                val cat = it.category.uppercase(Locale.getDefault())
                cat == "REPRODUCTEUR" || cat == "PRIMIPARE" || cat == "MULTIPARE"
            }
            R.id.chipEngraissement -> list.filter { it.category.equals("ENGRAISSEMENT", ignoreCase = true) }
            R.id.chipSevre -> list.filter { it.category.equals("SEVRE", ignoreCase = true) || it.category.equals("SÉVRÉS", ignoreCase = true) }
            R.id.chipVendu -> list.filter { it.category.equals("VENDU", ignoreCase = true) }
            R.id.chipDecede -> list.filter { it.category.equals("DECEDE", ignoreCase = true) || it.category.equals("DÉCÉDÉ", ignoreCase = true) }
            else -> list
        }

        // 2. Filter by search query
        if (currentSearchQuery.isNotEmpty()) {
            val queryLower = currentSearchQuery.lowercase(Locale.getDefault())
            list = list.filter { animal ->
                animal.tagNumber.lowercase(Locale.getDefault()).contains(queryLower) ||
                        (animal.breed?.lowercase(Locale.getDefault())?.contains(queryLower) == true)
            }
        }

        animalAdapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
