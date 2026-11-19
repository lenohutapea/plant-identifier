package com.lenohutapea.plantidentifier.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
import com.lenohutapea.plantidentifier.ui.ResultText
import com.lenohutapea.plantidentifier.ui.theme.PlantIcons
import com.lenohutapea.plantidentifier.ui.theme.PlantNameSmallStyle
import com.lenohutapea.plantidentifier.ui.theme.Verdict
import com.lenohutapea.plantidentifier.ui.theme.verdictColor
import com.lenohutapea.plantidentifier.ui.theme.verdictOf
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<DiagnosisEntity>,
    onOpen: (Long) -> Unit,
    onDelete: (DiagnosisEntity) -> Unit,
    onTakePhoto: () -> Unit,
    onBack: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<DiagnosisEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyHistory(Modifier.padding(padding), onTakePhoto)
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                HistoryRow(entry, onOpen = { onOpen(entry.id) }, onDelete = { pendingDelete = entry })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this diagnosis?") },
            text = { Text("The photo and result for ${entry.plantName} are removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun HistoryRow(entry: DiagnosisEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    val verdict = verdictOf(entry.conditionStatus, if (entry.conditionName == "Sehat") "healthy" else null)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeafImage(
            path = entry.imagePath,
            contentDescription = null,
            maxSide = 256,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (verdict == Verdict.Unrecognized) "Not recognized" else entry.plantName,
                style = PlantNameSmallStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusTag(verdict, statusText(entry))
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusTag(verdict: Verdict, text: String) {
    val color = verdictColor(verdict)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

private fun statusText(entry: DiagnosisEntity): String = when (entry.conditionStatus) {
    "assessed" -> "${entry.conditionName} ${ResultText.percent(entry.conditionConfidence ?: 0.0)}"
    "uncertain" -> "Possibly ${entry.conditionName}"
    "not_supported" -> "Condition not assessed"
    else -> "Plant not recognized"
}

@Composable
private fun EmptyHistory(modifier: Modifier, onTakePhoto: () -> Unit) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(PlantIcons.Leaf, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(56.dp))
            Text("No diagnoses yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Results you get are saved here, with their photo, so you can open them again without a connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onTakePhoto) {
                Icon(PlantIcons.Camera, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Photograph a leaf")
            }
        }
    }
}
