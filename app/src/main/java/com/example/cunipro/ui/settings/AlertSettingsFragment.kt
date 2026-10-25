package com.example.cunipro.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cunipro.databinding.FragmentAlertSettingsBinding
import com.example.cunipro.util.NotificationHelper
import com.example.cunipro.worker.ReproductionNotificationWorker

class AlertSettingsFragment : Fragment() {

    private var _binding: FragmentAlertSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertSettingsViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerTestNotification()
        } else {
            Toast.makeText(
                requireContext(),
                "Permission refusée. Les alertes système ne pourront pas s'afficher.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Switches setup and listeners
        viewModel.stockFaible.observe(viewLifecycleOwner) { enabled ->
            binding.switchStockFaible.isChecked = enabled
        }
        binding.switchStockFaible.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setStockFaible(isChecked)
        }

        viewModel.miseBas.observe(viewLifecycleOwner) { enabled ->
            binding.switchMiseBas.isChecked = enabled
        }
        binding.switchMiseBas.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setMiseBas(isChecked)
        }

        viewModel.sevrage.observe(viewLifecycleOwner) { enabled ->
            binding.switchSevrage.isChecked = enabled
        }
        binding.switchSevrage.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSevrage(isChecked)
        }

        viewModel.soins.observe(viewLifecycleOwner) { enabled ->
            binding.switchSoins.isChecked = enabled
        }
        binding.switchSoins.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSoins(isChecked)
        }

        viewModel.tachesImportantes.observe(viewLifecycleOwner) { enabled ->
            binding.switchTachesImportantes.isChecked = enabled
        }
        binding.switchTachesImportantes.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTachesImportantes(isChecked)
        }

        // Time Picker setup
        viewModel.morningTime.observe(viewLifecycleOwner) { time ->
            binding.tvMorningTime.text = time
        }
        binding.cardMorningTimePicker.setOnClickListener {
            showMorningTimePicker()
        }

        // Battery / Notification Instructions button
        binding.btnBatteryInstructions.setOnClickListener {
            showBatteryOptimizationDialog()
        }

        // Test Notification Button
        binding.btnTestNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationHelper.hasNotificationPermission(requireContext())
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                triggerTestNotification()
            }
        }
    }

    private fun triggerTestNotification() {
        // Envoi immédiat d'une notification système de test
        NotificationHelper.sendNotification(
            context = requireContext(),
            notificationId = 9999,
            title = "Test Notification CUNI PRO 🐇",
            message = "Les notifications pour la reproduction sont bien configurées sur votre téléphone !"
        )

        // Déclenchement du worker pour évaluer les alertes de reproduction actuelles
        val testWork = OneTimeWorkRequestBuilder<ReproductionNotificationWorker>().build()
        WorkManager.getInstance(requireContext()).enqueue(testWork)

        Toast.makeText(
            requireContext(),
            "Notification de test envoyée sur votre téléphone !",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showMorningTimePicker() {
        val currentTime = binding.tvMorningTime.text.toString().split(":")
        val hour = currentTime.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = currentTime.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            requireContext(),
            { _, h, m ->
                val formattedTime = String.format("%02d:%02d", h, m)
                viewModel.setMorningTime(formattedTime)
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Autoriser les rappels en arrière-plan")
            .setMessage(
                "Pour vous assurer de recevoir tous les rappels à l'heure exacte :\n\n" +
                        "1. Allez dans les Paramètres du téléphone > Applications > CUNI PRO.\n" +
                        "2. Activez l'option Autoriser les notifications.\n" +
                        "3. Allez dans Batterie ou Économie d'énergie.\n" +
                        "4. Sélectionnez « Sans restriction » ou désactivez l'optimisation de batterie pour CUNI PRO.\n\n" +
                        "Cela garantit que le téléphone ne bloque pas les rappels matinal."
            )
            .setPositiveButton("Compris", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
