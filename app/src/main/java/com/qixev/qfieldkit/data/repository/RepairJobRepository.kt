package com.qixev.qfieldkit.data.repository

import androidx.room.withTransaction
import com.qixev.qfieldkit.data.local.dao.InventoryItemDao
import com.qixev.qfieldkit.data.local.dao.RepairJobDao
import com.qixev.qfieldkit.data.local.database.FieldKitDatabase
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import com.qixev.qfieldkit.data.local.model.JobPartDetail
import com.qixev.qfieldkit.data.local.model.JobStatusTransitions
import com.qixev.qfieldkit.data.local.model.JobWithParts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepairJobRepository(
    private val database: FieldKitDatabase,
    private val repairJobDao: RepairJobDao,
    private val inventoryItemDao: InventoryItemDao
) {

    fun getAllJobs(): Flow<List<RepairJobEntity>> {
        return repairJobDao.getAllJobs()
    }

    fun getJobById(jobId: Long): Flow<RepairJobEntity?> {
        return repairJobDao.getJobByIdFlow(jobId)
    }

    fun getJobWithParts(jobId: Long): Flow<JobWithParts?> {
        return repairJobDao.getJobWithParts(jobId)
    }

    fun getJobPartDetails(jobId: Long): Flow<List<JobPartDetail>> {
        return repairJobDao.getPartsForJobFlow(jobId).map { parts ->
            parts.mapNotNull { crossRef ->
                val item = inventoryItemDao.getInventoryItemById(crossRef.inventoryItemId)
                if (item != null) {
                    JobPartDetail(partRef = crossRef, item = item)
                } else null
            }
        }
    }

    suspend fun createJob(
        customerName: String = "",
        customerPhone: String = "",
        deviceName: String = "",
        problemDescription: String = "",
        title: String = "",
        description: String = "",
        estimatedCost: Double = 0.0,
        finalCost: Double = 0.0,
        status: JobStatus = JobStatus.RECEIVED
    ): OperationResult<Long> {
        val effectiveTitle = title.ifBlank { deviceName }.trim()
        val effectiveDesc = description.ifBlank { problemDescription }.trim()
        val effectiveDevice = deviceName.ifBlank { effectiveTitle }.trim()
        val effectiveProblem = problemDescription.ifBlank { effectiveDesc }.trim()

        val now = System.currentTimeMillis()
        val job = RepairJobEntity(
            title = effectiveTitle,
            description = effectiveDesc,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deviceName = effectiveDevice,
            problemDescription = effectiveProblem,
            status = status,
            estimatedCost = estimatedCost,
            finalCost = finalCost,
            createdAt = now,
            updatedAt = now
        )
        val id = repairJobDao.insertJob(job)
        return OperationResult.Success(id)
    }

    suspend fun updateJobDetails(
        jobId: Long,
        customerName: String = "",
        customerPhone: String = "",
        deviceName: String = "",
        problemDescription: String = "",
        title: String = "",
        description: String = "",
        estimatedCost: Double = 0.0,
        finalCost: Double = 0.0
    ): OperationResult<Unit> = database.withTransaction {
        val existing = repairJobDao.getJobById(jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (existing.status == JobStatus.DELIVERED || existing.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Failure(RepairJobError.JobLocked(existing.status.name))
        }

        val effectiveTitle = title.ifBlank { deviceName.ifBlank { existing.displayTitle } }.trim()
        val effectiveDesc = description.ifBlank { problemDescription.ifBlank { existing.displayDescription } }.trim()
        val effectiveDevice = deviceName.ifBlank { effectiveTitle }.trim()
        val effectiveProblem = problemDescription.ifBlank { effectiveDesc }.trim()

        val updated = existing.copy(
            title = effectiveTitle,
            description = effectiveDesc,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deviceName = effectiveDevice,
            problemDescription = effectiveProblem,
            estimatedCost = estimatedCost,
            finalCost = finalCost,
            updatedAt = System.currentTimeMillis()
        )
        repairJobDao.updateJob(updated)
        OperationResult.Success(Unit)
    }

    suspend fun updateJobStatus(jobId: Long, newStatus: JobStatus): OperationResult<Unit> =
        database.withTransaction {
            val job = repairJobDao.getJobById(jobId)
                ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

            if (!JobStatusTransitions.isValidTransition(job.status, newStatus)) {
                return@withTransaction OperationResult.Failure(
                    RepairJobError.InvalidStatusTransition(job.status.name, newStatus.name)
                )
            }

            repairJobDao.updateJob(
                job.copy(
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
            OperationResult.Success(Unit)
        }

    suspend fun addPartToJob(
        jobId: Long,
        inventoryItemId: Long,
        quantity: Int,
        location: String? = null,
        purpose: String? = null
    ): OperationResult<Unit> = database.withTransaction {
        if (quantity <= 0) {
            return@withTransaction OperationResult.Failure(RepairJobError.InvalidQuantity(quantity))
        }

        val job = repairJobDao.getJobById(jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (job.status == JobStatus.DELIVERED || job.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Failure(RepairJobError.JobLocked(job.status.name))
        }

        val item = inventoryItemDao.getInventoryItemById(inventoryItemId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.InventoryItemNotFound)

        if (item.quantity < quantity) {
            return@withTransaction OperationResult.Failure(
                RepairJobError.InsufficientStock(
                    available = item.quantity,
                    requested = quantity
                )
            )
        }

        // 1. Decrement inventory stock
        val updatedStock = item.quantity - quantity
        inventoryItemDao.updateItem(item.copy(quantity = updatedStock))

        val now = System.currentTimeMillis()
        val locTrimmed = location?.trim()?.ifBlank { null }
        val purposeTrimmed = purpose?.trim()?.ifBlank { null }

        // 2. Insert new distinct usage event
        val newRef = JobPartCrossRef(
            jobId = jobId,
            inventoryItemId = inventoryItemId,
            quantityUsed = quantity,
            unitPriceAtTimeOfUse = item.sellingPrice,
            usedAt = now,
            location = locTrimmed,
            purpose = purposeTrimmed
        )
        repairJobDao.insertJobPart(newRef)

        // 3. Update Job updatedAt timestamp
        repairJobDao.updateJob(job.copy(updatedAt = now))

        OperationResult.Success(Unit)
    }

    suspend fun removeUsageRecord(usageId: Long): OperationResult<Unit> = database.withTransaction {
        val crossRef = repairJobDao.getJobPartById(usageId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.InventoryItemNotFound)

        val job = repairJobDao.getJobById(crossRef.jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (job.status == JobStatus.DELIVERED || job.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Failure(RepairJobError.JobLocked(job.status.name))
        }

        val item = inventoryItemDao.getInventoryItemById(crossRef.inventoryItemId)
        if (item != null) {
            // Restore inventory for this specific usage event
            val restoredQuantity = item.quantity + crossRef.quantityUsed
            inventoryItemDao.updateItem(item.copy(quantity = restoredQuantity))
        }

        // Delete specific usage event row
        repairJobDao.deleteJobPart(crossRef)

        // Update Job updatedAt
        repairJobDao.updateJob(job.copy(updatedAt = System.currentTimeMillis()))

        OperationResult.Success(Unit)
    }

    suspend fun removePartFromJob(
        jobId: Long,
        inventoryItemId: Long
    ): OperationResult<Unit> = database.withTransaction {
        val crossRef = repairJobDao.getJobPart(jobId, inventoryItemId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.InventoryItemNotFound)
        removeUsageRecord(crossRef.id)
    }

    suspend fun cancelJob(jobId: Long): OperationResult<Unit> = database.withTransaction {
        val job = repairJobDao.getJobById(jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (job.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Success(Unit)
        }

        if (job.status == JobStatus.DELIVERED) {
            return@withTransaction OperationResult.Failure(
                RepairJobError.InvalidStatusTransition(
                    from = JobStatus.DELIVERED.name,
                    to = JobStatus.CANCELLED.name
                )
            )
        }

        // 1. Retrieve all allocated job parts
        val parts = repairJobDao.getPartsForJob(jobId)

        // 2. Restore all quantities to inventory
        for (part in parts) {
            val item = inventoryItemDao.getInventoryItemById(part.inventoryItemId)
            if (item != null) {
                val restored = item.quantity + part.quantityUsed
                inventoryItemDao.updateItem(item.copy(quantity = restored))
            }
        }

        // 3. Delete all job-part rows
        repairJobDao.deleteAllPartsForJob(jobId)

        // 4. Update status to CANCELLED and update timestamp
        repairJobDao.updateJob(
            job.copy(
                status = JobStatus.CANCELLED,
                updatedAt = System.currentTimeMillis()
            )
        )

        OperationResult.Success(Unit)
    }
}

