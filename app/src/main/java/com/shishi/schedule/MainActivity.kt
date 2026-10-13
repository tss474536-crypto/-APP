package com.shishi.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shishi.schedule.ui.AppViewModel
import com.shishi.schedule.ui.ShishiApp
import com.shishi.schedule.ui.ShishiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShishiTheme {
                val model: AppViewModel = viewModel()
                ShishiApp(model)
            }
        }
    }
}
