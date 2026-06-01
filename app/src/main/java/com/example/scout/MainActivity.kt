package com.example.scout

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.scout.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (navController.currentDestination?.id != item.itemId && !navController.popBackStack(item.itemId, false)) {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
            true
        }

        binding.bottomNav.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        val fabDestinations = setOf(R.id.exploreFragment, R.id.favouritesFragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in fabDestinations) {
                binding.fab.show()
            } else {
                binding.fab.hide()
            }

            val menu = binding.bottomNav.menu
            when (destination.id) {
                R.id.speciesDetailFragment -> {
                // tab stays on previous selection
                }
                R.id.sightingsFragment -> {
                    menu.findItem(R.id.profileFragment)?.isChecked = true
                }
            }
        }
        binding.fab.setOnClickListener {
            navController.navigate(R.id.logSightingFragment)
        }
    }
}