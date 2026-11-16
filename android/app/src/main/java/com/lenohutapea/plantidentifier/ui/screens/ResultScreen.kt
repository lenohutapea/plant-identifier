package com.lenohutapea.plantidentifier.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenohutapea.plantidentifier.data.Diagnosis
import com.lenohutapea.plantidentifier.data.remote.ConditionInfo
import com.lenohutapea.plantidentifier.data.remote.PlantInfo
import com.lenohutapea.plantidentifier.data.remote.PredictResponse
import com.lenohutapea.plantidentifier.ui.ResultText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(diagnosis: Diagnosis, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val prediction = diagnosis.prediction
    val info = diagnosis.plantInfo
    val conditionInfo: ConditionInfo? = prediction.condition?.let { info?.conditions?.get(it.id) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LeafImage(
                    path = diagnosis.imagePath,
                    contentDescription = "Analyzed leaf photo",
                    maxSide = 1024,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
            item { PlantCard(prediction, info) }
            item { ConditionCard(prediction) }
            item { TopPredictionsCard(prediction) }

            if (prediction.recognized && info != null) {
                info.indonesiaNote?.let { note -> item { InfoCard("In Indonesia", note) } }
                item { InfoCard("How to plant", info.planting) }
                item { InfoCard("Water", info.water) }
                item { InfoCard("Fertilizer", info.fertilizer) }
                item { InfoCard("Harvest", info.harvest.summary) }
                if (!info.schedule.isNullOrEmpty()) {
                    item { ScheduleCard(info) }
                }
            } else if (prediction.recognized) {
                item {
                    InfoCard("Care information", "Care information could not be loaded. Check the connection and try again.")
                }
            }

            if (conditionInfo != null && conditionInfo.type != "healthy") {
                item { ConditionDetailCard(conditionInfo) }
            }

            item {
                Text(
                    prediction.disclaimer ?: info?.disclaimer ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                )
            }
            item {
                Text(
                    "Model ${prediction.modelVersion} · ${prediction.inferenceMs.toInt()} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlantCard(prediction: PredictResponse, info: PlantInfo?) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Plant", style = MaterialTheme.typography.labelLarge)
            if (prediction.recognized) {
                Text(prediction.plant.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                info?.let {
                    Text(it.scientificName, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
                }
                ConfidenceBar(prediction.plant.confidence)
            } else {
                Text("Not recognized", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(ResultText.statusExplanation("unrecognized", ""), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Closest guesses: " + prediction.topPlants.joinToString { "${it.name} ${ResultText.percent(it.confidence)}" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConditionCard(prediction: PredictResponse) {
    if (prediction.conditionStatus == "unrecognized") return
    val condition = prediction.condition
    val healthy = condition?.type == "healthy"
    val container = when {
        prediction.conditionStatus != "assessed" -> MaterialTheme.colorScheme.surfaceVariant
        healthy -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Leaf condition", style = MaterialTheme.typography.labelLarge)
            when {
                condition == null -> Text("Cannot be assessed", style = MaterialTheme.typography.titleLarge)
                prediction.conditionStatus == "uncertain" ->
                    Text("Possibly: ${condition.name}", style = MaterialTheme.typography.titleLarge)
                else -> Text(condition.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            condition?.let {
                Text(ResultText.conditionType(it.type), style = MaterialTheme.typography.bodyMedium)
                ConfidenceBar(it.confidence)
            }
            Text(
                ResultText.statusExplanation(prediction.conditionStatus, prediction.plant.name),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TopPredictionsCard(prediction: PredictResponse) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Top predictions", style = MaterialTheme.typography.labelLarge)
            prediction.topPredictions.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.plantName} — ${p.conditionName}", modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text(ResultText.percent(p.confidence), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(info: PlantInfo) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Planting schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            info.schedule.orEmpty().forEachIndexed { index, step ->
                if (index > 0) HorizontalDivider()
                Text(ResultText.scheduleDay(step.hst), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(step.activity, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ConditionDetailCard(condition: ConditionInfo) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(condition.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            condition.pathogen?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic) }
            Text(condition.description, style = MaterialTheme.typography.bodyMedium)
            BulletSection("Symptoms", condition.symptoms)
            BulletSection("Treatment", condition.treatment)
            BulletSection("Prevention", condition.prevention)
        }
    }
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(4.dp))
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConfidenceBar(value: Double) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { value.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = Color.LightGray.copy(alpha = 0.4f),
        )
        Spacer(Modifier.width(12.dp))
        Text(ResultText.percent(value), fontWeight = FontWeight.SemiBold)
    }
}
