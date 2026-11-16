package com.lenohutapea.plantidentifier.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
import com.lenohutapea.plantidentifier.ui.ResultText
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<DiagnosisEntity>,
    onOpen: (Long) -> Unit,
    onDelete: (DiagnosisEntity) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No diagnoses yet.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                HistoryRow(entry, onOpen = { onOpen(entry.id) }, onDelete = { onDelete(entry) })
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: DiagnosisEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LeafImage(
                path = entry.imagePath,
                contentDescription = null,
                maxSide = 256,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    if (entry.conditionStatus == "unrecognized") "Not recognized" else entry.plantName,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(conditionLine(entry), style = MaterialTheme.typography.bodyMedium)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun conditionLine(entry: DiagnosisEntity): String = when (entry.conditionStatus) {
    "assessed" -> "${entry.conditionName} (${ResultText.percent(entry.conditionConfidence ?: 0.0)})"
    "uncertain" -> "Possibly ${entry.conditionName}"
    "not_supported" -> "Condition not assessed"
    else -> "Plant not recognized"
}
