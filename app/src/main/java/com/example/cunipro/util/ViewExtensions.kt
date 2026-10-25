package com.example.cunipro.util

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

fun Fragment.setupTopActions(
    btnFinance: View?,
    btnNotifications: View?,
    btnSettings: View?,
    badgeView: TextView? = null
) {
    val navController = NavHostFragment.findNavController(this)
    val currentDestId = navController.currentDestination?.id

    fun updateTopCardSelection(card: View?, isSelected: Boolean) {
        if (card is MaterialCardView) {
            val density = card.resources.displayMetrics.density
            if (isSelected) {
                card.strokeWidth = (2.5f * density).toInt()
                card.strokeColor = ContextCompat.getColor(card.context, R.color.cuni_green_primary)
                card.setCardBackgroundColor(ContextCompat.getColor(card.context, R.color.cuni_green_container))
            } else {
                card.strokeWidth = 0
                card.setCardBackgroundColor(ContextCompat.getColor(card.context, R.color.cuni_green_light))
            }
        }
    }

    updateTopCardSelection(btnFinance, currentDestId == R.id.salesFinanceFragment)
    updateTopCardSelection(btnNotifications, currentDestId == R.id.tasksFragment)
    updateTopCardSelection(btnSettings, currentDestId == R.id.settingsFragment)

    btnFinance?.setOnClickListener {
        if (navController.currentDestination?.id != R.id.salesFinanceFragment) {
            navController.navigate(R.id.salesFinanceFragment)
        }
    }
    btnNotifications?.setOnClickListener {
        if (navController.currentDestination?.id != R.id.tasksFragment) {
            navController.navigate(R.id.tasksFragment)
        }
    }
    btnSettings?.setOnClickListener {
        if (navController.currentDestination?.id != R.id.settingsFragment) {
            navController.navigate(R.id.settingsFragment)
        }
    }

    if (badgeView != null) {
        setupTopNotificationBadge(badgeView)
    }
}

fun Fragment.setupTopNotificationBadge(badgeView: TextView?) {
    if (badgeView == null) return
    val app = requireActivity().application as CuniProApplication
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            combine(
                app.taskRepository.pendingTaskCount,
                app.notificationRepository.unreadCount
            ) { pendingTasks, unreadNotifications ->
                pendingTasks + unreadNotifications
            }.collect { total ->
                updateTopNotificationBadge(badgeView, total)
            }
        }
    }
}

fun updateTopNotificationBadge(badgeView: TextView?, count: Int) {
    if (badgeView == null) return
    if (count > 0) {
        badgeView.text = if (count > 99) "99+" else count.toString()
        badgeView.visibility = View.VISIBLE
    } else {
        badgeView.visibility = View.GONE
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.setupAutoRepeat(
    initialDelayMs: Long = 350L,
    repeatIntervalMs: Long = 75L,
    action: () -> Unit
) {
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null

    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacksAndMessages(null)
                v.isPressed = true
                action()

                runnable = object : Runnable {
                    override fun run() {
                        action()
                        handler.postDelayed(this, repeatIntervalMs)
                    }
                }
                runnable?.let { handler.postDelayed(it, initialDelayMs) }
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                runnable = null
                v.isPressed = false
                true
            }
            else -> false
        }
    }
}
