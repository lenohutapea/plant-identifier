package com.lenohutapea.plantidentifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lenohutapea.plantidentifier.ui.PlantIdentifierNavHost
import com.lenohutapea.plantidentifier.ui.theme.PlantIdentifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlantIdentifierTheme {
                // Themed background behind every screen, so no frame ever shows a bare white window.
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PlantIdentifierNavHost()
                }
            }
        }
    }
}
