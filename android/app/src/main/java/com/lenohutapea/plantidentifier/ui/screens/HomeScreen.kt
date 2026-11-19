package com.lenohutapea.plantidentifier.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
import com.lenohutapea.plantidentifier.data.remote.PlantSummary
import com.lenohutapea.plantidentifier.ui.AnalysisState
import com.lenohutapea.plantidentifier.ui.theme.Field
import com.lenohutapea.plantidentifier.ui.theme.PlantIcons
import com.lenohutapea.plantidentifier.ui.theme.PlantNameSmallStyle
import com.lenohutapea.plantidentifier.ui.theme.verdictColor
import com.lenohutapea.plantidentifier.ui.theme.verdictOf
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AnalysisState,
    recent: List<DiagnosisEntity>,
    plants: List<PlantSummary>,
    onImageSelected: (Uri) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecent: (Long) -> Unit,
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
    val busy = state is AnalysisState.Loading

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Plant Identifier") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = onOpenHistory) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                CapturePanel(
                    enabled = !busy,
                    onTakePhoto = {
                        val uri = newCameraUri(context)
                        pendingCameraUri = uri
                        takePicture.launch(uri)
                    },
                    onPickPhoto = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )

                if (state is AnalysisState.Error) {
                    ErrorNote(message = state.message, onDismiss = onDismissError)
                }

                if (recent.isNotEmpty()) {
                    Section(title = "Recent", action = "See all", onAction = onOpenHistory) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(recent, key = { it.id }) { entry -> RecentTile(entry) { onOpenRecent(entry.id) } }
                        }
                    }
                }

                if (plants.isNotEmpty()) {
                    Section(title = "Recognizes ${plants.size} crops") {
                        PlantChips(plants)
                    }
                }

                Section(title = "For a clear result") {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tip(PlantIcons.Leaf, "One leaf, filling most of the frame")
                        Tip(PlantIcons.Sprout, "Daylight, without strong shadows or flash glare")
                        Tip(PlantIcons.Lesion, "Spots and discoloration in sharp focus")
                    }
                }
            }
        }

        if (state is AnalysisState.Loading) AnalyzingOverlay(photo = state.photo)
    }
}

@Composable
private fun CapturePanel(enabled: Boolean, onTakePhoto: () -> Unit, onPickPhoto: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Field.LeafInk),
    ) {
        VeinDrawing(Modifier.matchParentSize())
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Photograph a leaf", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                "Get the plant, its leaf condition and how to care for it.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onTakePhoto,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Field.Paddy, contentColor = Color(0xFF13260A)),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Icon(PlantIcons.Camera, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Take photo", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onPickPhoto, enabled = enabled) {
                Icon(PlantIcons.Gallery, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose from gallery", color = Color.White)
            }
        }
    }
}

/** A large leaf with veins drawn faintly in the panel's corner. */
@Composable
private fun VeinDrawing(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
        val tint = Color.White.copy(alpha = 0.09f)
        val tip = Offset(w * 1.02f, h * 0.02f)
        val base = Offset(w * 0.55f, h * 1.05f)
        val outline = Path().apply {
            moveTo(base.x, base.y)
            cubicTo(w * 0.52f, h * 0.45f, w * 0.75f, h * 0.1f, tip.x, tip.y)
            cubicTo(w * 1.05f, h * 0.5f, w * 0.9f, h * 0.9f, base.x, base.y)
            close()
        }
        drawPath(outline, tint, style = stroke)
        drawLine(tint, base, tip, strokeWidth = 3f, cap = StrokeCap.Round)
        for (i in 1..5) {
            val t = i / 6f
            val p = Offset(base.x + (tip.x - base.x) * t, base.y + (tip.y - base.y) * t)
            drawLine(tint, p, Offset(p.x - w * 0.12f, p.y - h * 0.05f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(tint, p, Offset(p.x + w * 0.06f, p.y + h * 0.1f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun Section(title: String, action: String? = null, onAction: () -> Unit = {}, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (action != null) TextButton(onClick = onAction) { Text(action) }
        }
        content()
    }
}

@Composable
private fun RecentTile(entry: DiagnosisEntity, onClick: () -> Unit) {
    val verdict = verdictOf(entry.conditionStatus, if (entry.conditionName == "Sehat") "healthy" else null)
    Column(
        modifier = Modifier.width(132.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            LeafImage(
                path = entry.imagePath,
                contentDescription = null,
                maxSide = 300,
                modifier = Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(16.dp)),
            )
            Box(
                Modifier
                    .padding(8.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(verdictColor(verdict))
                    .border(2.dp, Color.White, CircleShape),
            )
        }
        Text(
            if (entry.conditionStatus == "unrecognized") "Not recognized" else entry.plantName,
            style = PlantNameSmallStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            when (entry.conditionStatus) {
                "assessed" -> entry.conditionName.orEmpty()
                "uncertain" -> "Possibly ${entry.conditionName.orEmpty()}"
                "not_supported" -> "Condition not assessed"
                else -> "Try another photo"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantChips(plants: List<PlantSummary>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        plants.sortedBy { it.name }.forEach { plant ->
            Text(
                plant.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Tip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorNote(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 16.dp, top = 14.dp, end = 8.dp, bottom = 4.dp),
    ) {
        Text("The photo was not analyzed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Dismiss") }
    }
}

/** A content:// URI for the camera app to write a full-size photo into the app's cache. */
private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
