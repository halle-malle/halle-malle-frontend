package com.do_it

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.do_it.databinding.ActivityMainBinding
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar4)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val navView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
                as NavHostFragment

        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home,
                R.id.quest,
                R.id.my
            )
        )

        setupActionBarWithNavController(
            navController,
            appBarConfiguration
        )
        navController.addOnDestinationChangedListener { _, _, _ ->
            supportActionBar?.setHomeAsUpIndicator(R.drawable.outline_arrow_back_ios_24)
        }
        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.my) {
                showMyPage()
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
        navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.my) showMyPage()
        }

        binding.btnNotification.setOnClickListener {
            navController.navigate(R.id.notification_SettingsFragment)
        }


    }
    private fun showMyPage() {
        if (navController.currentDestination?.id == R.id.my) return
        if (navController.popBackStack(R.id.my, false)) return
        navController.navigate(R.id.my, null, navOptions {
            launchSingleTop = true
            restoreState = false
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) ||
                super.onSupportNavigateUp()
    }
}
