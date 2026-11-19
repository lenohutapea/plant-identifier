package com.lenohutapea.plantidentifier.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** What a diagnosis means for the reader, independent of how the API names it. */
enum class Verdict { Healthy, Disease, Uncertain, NotAssessed, Unrecognized }

fun verdictOf(conditionStatus: String, conditionType: String?): Verdict = when (conditionStatus) {
    "assessed" -> if (conditionType == "healthy") Verdict.Healthy else Verdict.Disease
    "uncertain" -> Verdict.Uncertain
    "not_supported" -> Verdict.NotAssessed
    else -> Verdict.Unrecognized
}

@Composable
fun verdictColor(verdict: Verdict): Color = when (verdict) {
    Verdict.Healthy -> if (MaterialTheme.colorScheme.background.luminanceIsDark()) Color(0xFF8FD18F) else Field.Healthy
    Verdict.Disease -> MaterialTheme.colorScheme.error
    Verdict.Uncertain -> MaterialTheme.colorScheme.tertiary
    Verdict.NotAssessed, Verdict.Unrecognized -> MaterialTheme.colorScheme.outline
}

private fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
