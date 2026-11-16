package com.lenohutapea.plantidentifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lenohutapea.plantidentifier.ui.PlantIdentifierNavHost
import com.lenohutapea.plantidentifier.ui.theme.PlantIdentifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantIdentifierTheme {
                PlantIdentifierNavHost()
            }
        }
    }
}
