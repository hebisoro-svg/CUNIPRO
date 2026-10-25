package com.example.cunipro

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cunipro.databinding.ActivityMainBinding
import com.example.cunipro.util.NotificationHelper
import com.example.cunipro.worker.ReproductionNotificationWorker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val immediateWork = OneTimeWorkRequestBuilder<ReproductionNotificationWorker>().build()
            WorkManager.getInstance(this).enqueue(immediateWork)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar to white with dark icons for high legibility
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.animalsFragment,
                R.id.reproductionFragment,
                R.id.feedFragment,
                R.id.healthFinanceFragment
            )
        )

        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        val app = application as CuniProApplication
        val prefs = getSharedPreferences("cunipro_prefs", MODE_PRIVATE)
        val isRegistered = prefs.getBoolean("user_registered", false)
        val firebaseLoggedIn = runCatching { app.authRepository.isUserLoggedIn() }.getOrDefault(false)
        val isLoggedPref = if (prefs.contains("is_logged_in")) {
            prefs.getBoolean("is_logged_in", false)
        } else {
            val legacyLoggedIn = isRegistered && firebaseLoggedIn
            if (legacyLoggedIn) prefs.edit().putBoolean("is_logged_in", true).apply()
            legacyLoggedIn
        }
        val isLoggedIn = isLoggedPref && firebaseLoggedIn

        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            val startDest = when {
                isLoggedIn -> R.id.dashboardFragment
                isRegistered -> R.id.loginFragment
                else -> R.id.registerFragment
            }
            navGraph.setStartDestination(startDest)
            navController.graph = navGraph
        }

        val topLevelDestinations = setOf(
            R.id.dashboardFragment,
            R.id.animalsFragment,
            R.id.reproductionFragment,
            R.id.feedFragment,
            R.id.healthFinanceFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide top app bar everywhere so each fragment uses its own clean header
            binding.appBarLayout.visibility = View.GONE

            if (destination.id in topLevelDestinations) {
                binding.bottomNavigation.visibility = View.VISIBLE
            } else {
                binding.bottomNavigation.visibility = View.GONE
            }
        }

        setupEmojiMenuIcons()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController) || super.onOptionsItemSelected(item)
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.tasksFragment) {
                if (navController.currentDestination?.id != R.id.tasksFragment) {
                    navController.navigate(R.id.tasksFragment)
                }
                true
            } else {
                false
            }
        }

        checkAndRequestNotificationPermission()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupEmojiMenuIcons() {
        binding.bottomNavigation.itemIconTintList = null

        val bottomMenu = binding.bottomNavigation.menu
        bottomMenu.findItem(R.id.dashboardFragment)?.icon = createEmojiDrawable("🏠")
        bottomMenu.findItem(R.id.animalsFragment)?.icon = createEmojiDrawable("🐇")
        bottomMenu.findItem(R.id.reproductionFragment)?.icon = createEmojiDrawable("💖")
        bottomMenu.findItem(R.id.feedFragment)?.icon = createEmojiDrawable("🌾")
        bottomMenu.findItem(R.id.healthFinanceFragment)?.icon = createEmojiDrawable("🩺")

        binding.topAppBar.menu.findItem(R.id.tasksFragment)?.icon = createEmojiDrawable("📋")
    }

    private fun createEmojiDrawable(emoji: String, sizeDp: Float = 28f): Drawable {
        val density = resources.displayMetrics.density
        val px = (sizeDp * density).toInt()
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = px * 0.78f
            textAlign = Paint.Align.CENTER
        }
        val fontMetrics = paint.fontMetrics
        val y = (px / 2f) - ((fontMetrics.descent + fontMetrics.ascent) / 2f)
        canvas.drawText(emoji, px / 2f, y, paint)
        return BitmapDrawable(resources, bitmap)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
