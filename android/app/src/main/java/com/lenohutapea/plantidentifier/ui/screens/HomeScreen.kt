package com.lenohutapea.plantidentifier.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lenohutapea.plantidentifier.ui.AnalysisState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AnalysisState,
    onImageSelected: (Uri) -> Unit,
    onOpenHistory: () -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) onImageSelected(uri)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImageSelected(uri)
    }
    val loading = state is AnalysisState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plant Identifier") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Identify a crop and its leaf condition",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Take a photo of one leaf. The app tells you the plant, whether the leaf looks healthy " +
                        "or diseased, and how to care for it (care information is in Indonesian).",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tips for a good photo", fontWeight = FontWeight.SemiBold)
                        Text("• One leaf, filling most of the frame")
                        Text("• Daylight, no strong shadows or flash glare")
                        Text("• Show the spots or discoloration clearly")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val uri = newCameraUri(context)
                        pendingCameraUri = uri
                        takePicture.launch(uri)
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Take photo") }
                OutlinedButton(
                    onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text("Choose from gallery") }

                if (state is AnalysisState.Error) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Analysis failed", fontWeight = FontWeight.SemiBold)
                            Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onDismissError, modifier = Modifier.align(Alignment.End)) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            if (loading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Analyzing leaf…")
                }
            }
        }
    }
}

/** A content:// URI for the camera app to write a full-size photo into the app's cache. */
private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
