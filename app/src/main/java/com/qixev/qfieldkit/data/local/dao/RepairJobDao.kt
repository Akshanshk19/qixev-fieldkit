package com.qixev.qfieldkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.JobWithParts
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairJobDao {

    @Query("SELECT * FROM repair_jobs ORDER BY updatedAt DESC")
    fun getAllJobs(): Flow<List<RepairJobEntity>>

    @Query("SELECT * FROM repair_jobs ORDER BY id ASC")
    suspend fun getAllJobsSync(): List<RepairJobEntity>

    @Query("SELECT * FROM repair_jobs WHERE id = :jobId LIMIT 1")
    fun getJobByIdFlow(jobId: Long): Flow<RepairJobEntity?>

    @Query("SELECT * FROM repair_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobById(jobId: Long): RepairJobEntity?

    @Transaction
    @Query("SELECT * FROM repair_jobs WHERE id = :jobId LIMIT 1")
    fun getJobWithParts(jobId: Long): Flow<JobWithParts?>

    @Transaction
    @Query("SELECT * FROM repair_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobWithPartsSync(jobId: Long): JobWithParts?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJob(job: RepairJobEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllJobs(jobs: List<RepairJobEntity>)

    @Update
    suspend fun updateJob(job: RepairJobEntity)

    @Delete
    suspend fun deleteJob(job: RepairJobEntity)

    @Query("DELETE FROM repair_jobs")
    suspend fun deleteAllJobs()

    // Job Parts / Inventory Usage Events
    @Query("SELECT * FROM job_parts WHERE id = :usageId LIMIT 1")
    suspend fun getJobPartById(usageId: Long): JobPartCrossRef?

    @Query("SELECT * FROM job_parts WHERE jobId = :jobId AND inventoryItemId = :inventoryItemId LIMIT 1")
    suspend fun getJobPart(jobId: Long, inventoryItemId: Long): JobPartCrossRef?

    @Query("SELECT * FROM job_parts WHERE jobId = :jobId ORDER BY usedAt ASC, id ASC")
    suspend fun getPartsForJob(jobId: Long): List<JobPartCrossRef>

    @Query("SELECT * FROM job_parts ORDER BY jobId ASC, id ASC")
    suspend fun getAllJobPartsSync(): List<JobPartCrossRef>

    @Query("SELECT * FROM job_parts WHERE jobId = :jobId ORDER BY usedAt ASC, id ASC")
    fun getPartsForJobFlow(jobId: Long): Flow<List<JobPartCrossRef>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJobPart(jobPart: JobPartCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllJobParts(parts: List<JobPartCrossRef>)

    @Update
    suspend fun updateJobPart(jobPart: JobPartCrossRef)

    @Delete
    suspend fun deleteJobPart(jobPart: JobPartCrossRef)

    @Query("DELETE FROM job_parts WHERE id = :usageId")
    suspend fun deleteJobPartById(usageId: Long)

    @Query("DELETE FROM job_parts WHERE jobId = :jobId")
    suspend fun deleteAllPartsForJob(jobId: Long)

    @Query("DELETE FROM job_parts")
    suspend fun deleteAllJobParts()
}

