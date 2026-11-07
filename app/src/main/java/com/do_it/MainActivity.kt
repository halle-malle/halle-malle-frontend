package com.do_it


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.do_it.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var showBottomSheet by mutableStateOf(false)

    //ModalBottomSheet가 실험 API라서 "알고 쓴다"는 표시가 필요
    @OptIn(ExperimentalMaterial3Api::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar4)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val navView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main)
                as NavHostFragment

        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
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

        navView.setupWithNavController(navController)

        binding.btnNotification.setOnClickListener {
            Toast.makeText(this, "알림 클릭", Toast.LENGTH_SHORT).show()
        }




        binding.composeView.setContent {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    sheetState = sheetState
                ) {
                    // Sheet content
                    Button(onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }) {
                        Text("Hide bottom sheet")
                    }
                }
            }
        }
        binding.btnSettings.setOnClickListener {
            showBottomSheet = true
        }
    }
}
