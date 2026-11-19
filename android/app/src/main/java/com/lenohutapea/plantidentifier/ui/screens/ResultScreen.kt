package com.lenohutapea.plantidentifier.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lenohutapea.plantidentifier.data.Diagnosis
import com.lenohutapea.plantidentifier.data.remote.ConditionInfo
import com.lenohutapea.plantidentifier.data.remote.PlantInfo
import com.lenohutapea.plantidentifier.data.remote.PredictResponse
import com.lenohutapea.plantidentifier.ui.ResultText
import com.lenohutapea.plantidentifier.ui.theme.LatinNameStyle
import com.lenohutapea.plantidentifier.ui.theme.PlantIcons
import com.lenohutapea.plantidentifier.ui.theme.PlantNameStyle
import com.lenohutapea.plantidentifier.ui.theme.Verdict
import com.lenohutapea.plantidentifier.ui.theme.verdictColor
import com.lenohutapea.plantidentifier.ui.theme.verdictOf
import java.text.DateFormat
import java.util.Date

private val Tabs = listOf("Diagnosis", "Care", "Schedule")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(diagnosis: Diagnosis, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val prediction = diagnosis.prediction
    val info = diagnosis.plantInfo
    val verdict = verdictOf(prediction.conditionStatus, prediction.condition?.type)
    val conditionInfo = prediction.condition?.let { info?.conditions?.get(it.id) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                LeafImage(
                    path = diagnosis.imagePath,
                    contentDescription = "Analyzed leaf photo",
                    maxSide = 1024,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(24.dp)),
                )
            }
            item { SpecimenLabel(diagnosis, info, verdict) }

            if (!prediction.recognized) {
                item { TopPredictions(prediction, title = "Closest matches") }
            } else {
                item {
                    PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                        Tabs.forEachIndexed { index, title ->
                            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                        }
                    }
                }
                when (tab) {
                    0 -> diagnosisTab(prediction, conditionInfo, verdict)
                    1 -> careTab(info)
                    else -> scheduleTab(info)
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        prediction.disclaimer ?: info?.disclaimer.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Model ${prediction.modelVersion}, ${prediction.inferenceMs.toInt()} ms on the server",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The plant's "herbarium label": name, scientific name, confidence and leaf condition. */
@Composable
private fun SpecimenLabel(diagnosis: Diagnosis, info: PlantInfo?, verdict: Verdict) {
    val prediction = diagnosis.prediction
    val accent = verdictColor(verdict)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (prediction.recognized) {
            Text(prediction.plant.name, style = PlantNameStyle, color = MaterialTheme.colorScheme.onSurface)
            info?.let { Text(it.scientificName, style = LatinNameStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(10.dp))
            Meter(label = "Plant", value = prediction.plant.confidence, color = MaterialTheme.colorScheme.primary)
        } else {
            Text("Not recognized", style = PlantNameStyle, color = MaterialTheme.colorScheme.onSurface)
            Text(
                ResultText.statusExplanation("unrecognized", ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (verdict != Verdict.Unrecognized) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (verdict == Verdict.Healthy) PlantIcons.Shield else PlantIcons.Lesion,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(conditionHeadline(prediction, verdict), style = MaterialTheme.typography.titleLarge, color = accent)
                    prediction.condition?.let {
                        Text(ResultText.conditionType(it.type), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            prediction.condition?.let {
                Spacer(Modifier.height(8.dp))
                Meter(label = "Condition", value = it.confidence, color = accent)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                ResultText.statusExplanation(prediction.conditionStatus, prediction.plant.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(diagnosis.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun conditionHeadline(prediction: PredictResponse, verdict: Verdict): String = when (verdict) {
    Verdict.Healthy, Verdict.Disease -> prediction.condition?.name.orEmpty()
    Verdict.Uncertain -> "Possibly ${prediction.condition?.name.orEmpty()}"
    Verdict.NotAssessed -> "Condition not assessed"
    Verdict.Unrecognized -> ""
}

@Composable
private fun Meter(label: String, value: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(76.dp))
        LinearProgressIndicator(
            progress = { value.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {},
        )
        Text(
            ResultText.percent(value),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(52.dp).padding(start = 10.dp),
        )
    }
}

private fun LazyListScope.diagnosisTab(prediction: PredictResponse, condition: ConditionInfo?, verdict: Verdict) {
    if (condition != null && verdict != Verdict.Healthy) {
        item { InfoBlock(PlantIcons.Lesion, condition.name, condition.description, subtitle = condition.pathogen) }
        if (condition.symptoms.isNotEmpty()) item { BulletBlock("Symptoms", condition.symptoms) }
        if (condition.treatment.isNotEmpty()) item { BulletBlock("Treatment", condition.treatment) }
        if (condition.prevention.isNotEmpty()) item { BulletBlock("Prevention", condition.prevention) }
    } else if (condition != null) {
        item { InfoBlock(PlantIcons.Shield, condition.name, condition.description) }
        if (condition.prevention.isNotEmpty()) item { BulletBlock("Keep it healthy", condition.prevention) }
    }
    item { TopPredictions(prediction, title = "Top predictions") }
}

private fun LazyListScope.careTab(info: PlantInfo?) {
    if (info == null) {
        item { Text("Care information could not be loaded. Check the connection and open this result again.") }
        return
    }
    info.indonesiaNote?.let { note -> item { InfoBlock(PlantIcons.Place, "In Indonesia", note) } }
    item { InfoBlock(PlantIcons.Sprout, "Planting", info.planting) }
    item { InfoBlock(PlantIcons.Water, "Water", info.water) }
    item { InfoBlock(PlantIcons.Fertilizer, "Fertilizer", info.fertilizer) }
    item { InfoBlock(PlantIcons.Harvest, "Harvest", info.harvest.summary) }
}

private fun LazyListScope.scheduleTab(info: PlantInfo?) {
    val steps = info?.schedule.orEmpty()
    if (steps.isEmpty()) {
        item {
            Text(
                "This is a perennial crop: its calendar follows pruning and flowering rather than days after " +
                    "planting. See the Care tab for when it bears fruit.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    item {
        Text(
            "Days after planting (HST). Negative days are seedbed work before transplanting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    steps.forEachIndexed { index, step ->
        item { TimelineStep(ResultText.scheduleDay(step.hst), step.activity, isFirst = index == 0, isLast = index == steps.lastIndex) }
    }
}

@Composable
private fun TimelineStep(day: String, activity: String, isFirst: Boolean, isLast: Boolean) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(top = if (isFirst) 10.dp else 0.dp, bottom = if (isLast) 0.dp else 0.dp)
                    .background(if (isLast) Color.Transparent else line),
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.padding(bottom = 18.dp)) {
            Text(day, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(activity, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoBlock(icon: ImageVector, title: String, body: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BulletBlock(title: String, items: List<String>) {
    Column(Modifier.padding(start = 54.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        items.forEach { line ->
            Row {
                Text("•", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(14.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TopPredictions(prediction: PredictResponse, title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        prediction.topPredictions.forEach { p ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("${p.plantName}: ${p.conditionName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(ResultText.percent(p.confidence), style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { p.confidence.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
