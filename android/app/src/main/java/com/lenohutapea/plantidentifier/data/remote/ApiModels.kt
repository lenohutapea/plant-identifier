package com.lenohutapea.plantidentifier.data.remote

import com.google.gson.annotations.SerializedName

/** Response of POST /predict. */
data class PredictResponse(
    val recognized: Boolean,
    val plant: PlantPrediction,
    @SerializedName("condition_status") val conditionStatus: String,
    val condition: ConditionPrediction?,
    @SerializedName("top_predictions") val topPredictions: List<ClassPrediction>,
    @SerializedName("top_plants") val topPlants: List<PlantPrediction>,
    @SerializedName("model_version") val modelVersion: String,
    @SerializedName("inference_ms") val inferenceMs: Double,
    val disclaimer: String?,
)

data class PlantPrediction(
    val id: String,
    val name: String,
    val confidence: Double,
)

data class ConditionPrediction(
    val id: String,
    val label: String,
    val name: String,
    val type: String,
    val confidence: Double,
)

data class ClassPrediction(
    val label: String,
    @SerializedName("plant_id") val plantId: String,
    @SerializedName("plant_name") val plantName: String,
    @SerializedName("condition_id") val conditionId: String,
    @SerializedName("condition_name") val conditionName: String,
    val confidence: Double,
)

/** Response of GET /plants/{id}. Text fields are in Indonesian. */
data class PlantInfo(
    val id: String,
    val name: String,
    @SerializedName("scientific_name") val scientificName: String,
    val category: String,
    @SerializedName("indonesia_note") val indonesiaNote: String?,
    val planting: String,
    val water: String,
    val fertilizer: String,
    val harvest: Harvest,
    val schedule: List<ScheduleItem>?,
    val conditions: Map<String, ConditionInfo>,
    @SerializedName("condition_supported") val conditionSupported: Boolean,
    val disclaimer: String?,
)

data class Harvest(val summary: String)

data class ScheduleItem(val hst: Int, val activity: String)

data class ConditionInfo(
    val name: String,
    val type: String,
    val pathogen: String?,
    val description: String,
    val symptoms: List<String>,
    val treatment: List<String>,
    val prevention: List<String>,
)
