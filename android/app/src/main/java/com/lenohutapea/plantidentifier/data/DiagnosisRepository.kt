package com.lenohutapea.plantidentifier.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.lenohutapea.plantidentifier.data.local.DiagnosisDao
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
import com.lenohutapea.plantidentifier.data.remote.PlantApi
import com.lenohutapea.plantidentifier.data.remote.PlantInfo
import com.lenohutapea.plantidentifier.data.remote.PlantSummary
import com.lenohutapea.plantidentifier.data.remote.PredictResponse
import com.lenohutapea.plantidentifier.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/** A diagnosis as shown on the result screen. */
data class Diagnosis(
    val id: Long,
    val imagePath: String,
    val createdAt: Long,
    val prediction: PredictResponse,
    val plantInfo: PlantInfo?,
)

class DiagnosisRepository(
    private val context: Context,
    private val api: PlantApi,
    private val dao: DiagnosisDao,
) {
    private val gson = Gson()
    private val imageDir = File(context.filesDir, "images")

    val history: Flow<List<DiagnosisEntity>> = dao.observeAll()

    /** Compress the photo, send it to the API, fetch care info, and save the result to history. */
    suspend fun diagnose(source: Uri): Diagnosis = withContext(Dispatchers.IO) {
        val image = ImageCompressor.compress(context, source, imageDir)
        try {
            val part = MultipartBody.Part.createFormData(
                "image", image.name, image.asRequestBody("image/jpeg".toMediaType()),
            )
            val prediction = api.predict(part)
            // Care info is only meaningful when the plant was recognized; failure here is not fatal.
            val info = if (prediction.recognized) runCatching { api.plant(prediction.plant.id) }.getOrNull() else null
            val entity = DiagnosisEntity(
                createdAt = System.currentTimeMillis(),
                imagePath = image.absolutePath,
                plantId = prediction.plant.id,
                plantName = prediction.plant.name,
                plantConfidence = prediction.plant.confidence,
                conditionStatus = prediction.conditionStatus,
                conditionName = prediction.condition?.name,
                conditionConfidence = prediction.condition?.confidence,
                predictionJson = gson.toJson(prediction),
                plantInfoJson = info?.let { gson.toJson(it) },
            )
            val id = dao.insert(entity)
            Diagnosis(id, image.absolutePath, entity.createdAt, prediction, info)
        } catch (e: Exception) {
            image.delete()
            throw e
        }
    }

    /** Plants the deployed model recognizes (follows the model version on the server). */
    suspend fun supportedPlants(): List<PlantSummary> = withContext(Dispatchers.IO) { api.plants().plants }

    suspend fun load(id: Long): Diagnosis? = withContext(Dispatchers.IO) {
        dao.get(id)?.let { entity ->
            Diagnosis(
                id = entity.id,
                imagePath = entity.imagePath,
                createdAt = entity.createdAt,
                prediction = gson.fromJson(entity.predictionJson, PredictResponse::class.java),
                plantInfo = entity.plantInfoJson?.let { gson.fromJson(it, PlantInfo::class.java) },
            )
        }
    }

    suspend fun delete(entity: DiagnosisEntity) = withContext(Dispatchers.IO) {
        dao.delete(entity)
        File(entity.imagePath).delete()
    }
}
