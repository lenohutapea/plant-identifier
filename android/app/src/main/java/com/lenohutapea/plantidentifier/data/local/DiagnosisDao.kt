package com.lenohutapea.plantidentifier.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosisDao {
    @Query("SELECT * FROM diagnoses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiagnosisEntity>>

    @Query("SELECT * FROM diagnoses WHERE id = :id")
    suspend fun get(id: Long): DiagnosisEntity?

    @Insert
    suspend fun insert(entity: DiagnosisEntity): Long

    @Delete
    suspend fun delete(entity: DiagnosisEntity)
}
