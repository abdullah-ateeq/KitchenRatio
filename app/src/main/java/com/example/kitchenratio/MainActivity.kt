package com.example.kitchenratio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.kitchenratio.ui.KitchenRatioApp
import com.example.kitchenratio.ui.KitchenRatioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KitchenRatioViewModel by viewModels { KitchenRatioViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KitchenRatioApp(viewModel)
        }
    }
}
