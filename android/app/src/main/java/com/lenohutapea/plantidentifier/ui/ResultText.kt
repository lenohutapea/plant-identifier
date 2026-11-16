package com.lenohutapea.plantidentifier.ui

import kotlin.math.roundToInt

/** Pure text helpers for the result screen (unit-tested, no Android dependencies). */
object ResultText {
    fun percent(value: Double): String = "${(value * 100).roundToInt()}%"

    fun conditionType(type: String): String = when (type) {
        "healthy" -> "Healthy"
        "fungal" -> "Fungal disease"
        "bacterial" -> "Bacterial disease"
        "viral" -> "Viral disease"
        "oomycete" -> "Water mold (oomycete)"
        "pest" -> "Pest damage"
        else -> type.replaceFirstChar { it.uppercase() }
    }

    fun scheduleDay(hst: Int): String = when {
        hst < 0 -> "${-hst} days before planting"
        hst == 0 -> "Planting day"
        else -> "Day $hst after planting"
    }

    fun statusExplanation(status: String, plantName: String): String = when (status) {
        "assessed" -> "Leaf condition identified with good confidence."
        "uncertain" -> "Low confidence. Retake the photo: one leaf, close up, in daylight, filling most of the frame."
        "not_supported" -> "The training data does not cover both healthy and diseased leaves of $plantName, " +
            "so only the plant and its care information are shown."
        "unrecognized" -> "This does not look like one of the supported crops. Try a single leaf, close up, " +
            "on a plain background."
        else -> ""
    }
}
