package com.example.cunipro.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.databinding.DialogAddTaskBinding
import com.example.cunipro.databinding.FragmentTasksBinding
import com.example.cunipro.ui.adapter.NotificationAdapter
import com.example.cunipro.ui.adapter.TaskAdapter
import com.example.cunipro.util.BreedingDateCalculator
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.setupTopActions
import java.util.Calendar

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels {
        val app = requireActivity().application as CuniProApplication
        TasksViewModelFactory(app, app.taskRepository, app.notificationRepository)
    }

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupTopActions(
            binding.includeTopActions.btnTopFinance,
            binding.includeTopActions.btnTopNotifications,
            binding.includeTopActions.btnTopSettings,
            binding.includeTopActions.tvNotificationBadge
        )

        setupRecyclerViews()
        setupTabSwitching()
        observeData()

        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllNotificationsAsRead()
            Toast.makeText(requireContext(), "Toutes les notifications ont été marquées comme lues", Toast.LENGTH_SHORT).show()
        }

        binding.fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun setupRecyclerViews() {
        taskAdapter = TaskAdapter { task, isChecked ->
            viewModel.markTaskCompleted(task, isChecked)
        }
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }

        notificationAdapter = NotificationAdapter { notification ->
            viewModel.markNotificationAsRead(notification.id)
        }
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun setupTabSwitching() {
        binding.btnTabReceived.setOnClickListener {
            viewModel.selectTab(0)
        }
        binding.btnTabTasks.setOnClickListener {
            viewModel.selectTab(1)
        }
    }

    private fun observeData() {
        viewModel.selectedTab.observe(viewLifecycleOwner) { tab ->
            if (tab == 0) {
                // Reçues selected
                binding.btnTabReceived.setBackgroundResource(R.drawable.bg_pill_selected)
                binding.btnTabReceived.setTextColor(Color.WHITE)

                binding.btnTabTasks.setBackgroundColor(Color.TRANSPARENT)
                binding.btnTabTasks.setTextColor(Color.parseColor("#1C1B1F"))

                binding.layoutReceivedView.visibility = View.VISIBLE
                binding.layoutTasksView.visibility = View.GONE
            } else {
                // Tâches selected
                binding.btnTabTasks.setBackgroundResource(R.drawable.bg_pill_selected)
                binding.btnTabTasks.setTextColor(Color.WHITE)

                binding.btnTabReceived.setBackgroundColor(Color.TRANSPARENT)
                binding.btnTabReceived.setTextColor(Color.parseColor("#1C1B1F"))

                binding.layoutReceivedView.visibility = View.GONE
                binding.layoutTasksView.visibility = View.VISIBLE
            }
        }

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
            val activeCount = tasks.count { !it.isCompleted }
            val isFr = LanguageManager.isFrench(requireContext())
            binding.btnTabTasks.text = if (isFr) "Tâches · $activeCount" else "Tasks · $activeCount"
            binding.btnTabReceived.text = if (isFr) "Reçues" else "Received"
        }

        viewModel.allNotifications.observe(viewLifecycleOwner) { notifications ->
            notificationAdapter.submitList(notifications)
        }

        viewModel.unreadNotificationCount.observe(viewLifecycleOwner) { unread ->
            val isFr = LanguageManager.isFrench(requireContext())
            binding.tvUnreadCount.text = if (unread > 0) {
                if (isFr) "$unread non lues" else "$unread unread"
            } else {
                if (isFr) "Aucune non lue" else "None unread"
            }
        }
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val isFr = LanguageManager.isFrench(requireContext())

        // Spinners
        val categories = if (isFr) {
            arrayOf("Vente prévue", "Alimentation", "Soin", "Saillie", "Sevrage", "Autre")
        } else {
            arrayOf("Planned sale", "Feed", "Care", "Mating", "Weaning", "Other")
        }

        val reminders = if (isFr) {
            arrayOf("le jour même", "la veille", "2 jours avant", "3 jours avant")
        } else {
            arrayOf("same day", "day before", "2 days before", "3 days before")
        }

        val repeats = if (isFr) {
            arrayOf("Une seule fois", "Chaque jour", "Chaque semaine", "Chaque mois")
        } else {
            arrayOf("Once", "Every day", "Every week", "Every month")
        }

        dialogBinding.spCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        dialogBinding.spReminder.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, reminders)
        dialogBinding.spRepeat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, repeats)

        val cal = Calendar.getInstance()
        val todayStr = BreedingDateCalculator.getTodayString()
        dialogBinding.etTaskDate.setText(todayStr)
        dialogBinding.etTaskTime.setText("08:00")

        dialogBinding.etTaskDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                    dialogBinding.etTaskDate.setText(dateStr)
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.etTaskTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, min ->
                    dialogBinding.etTaskTime.setText(String.format("%02d:%02d", hour, min))
                },
                8, 0, true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Ajouter") { _, _ ->
                val title = dialogBinding.etTaskTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    val category = dialogBinding.spCategory.selectedItem.toString()
                    val date = dialogBinding.etTaskDate.text.toString().ifEmpty { todayStr }
                    val time = dialogBinding.etTaskTime.text.toString().ifEmpty { "08:00" }
                    val reminder = dialogBinding.spReminder.selectedItem.toString()
                    val repeat = dialogBinding.spRepeat.selectedItem.toString().let {
                        if (it == "Une seule fois") null else it.lowercase()
                    }
                    val isFlagged = dialogBinding.switchFlagged.isChecked

                    viewModel.addTask(title, category, date, time, reminder, repeat, isFlagged)
                    Toast.makeText(requireContext(), "Tâche ajoutée", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Veuillez entrer un titre", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
