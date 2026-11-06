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
        customerName: String,
        customerPhone: String,
        deviceName: String,
        problemDescription: String,
        estimatedCost: Double = 0.0,
        finalCost: Double = 0.0,
        status: JobStatus = JobStatus.RECEIVED
    ): OperationResult<Long> {
        val now = System.currentTimeMillis()
        val job = RepairJobEntity(
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deviceName = deviceName.trim(),
            problemDescription = problemDescription.trim(),
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
        customerName: String,
        customerPhone: String,
        deviceName: String,
        problemDescription: String,
        estimatedCost: Double,
        finalCost: Double
    ): OperationResult<Unit> = database.withTransaction {
        val existing = repairJobDao.getJobById(jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (existing.status == JobStatus.DELIVERED || existing.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Failure(RepairJobError.JobLocked(existing.status.name))
        }

        val updated = existing.copy(
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            deviceName = deviceName.trim(),
            problemDescription = problemDescription.trim(),
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
        quantity: Int
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

        // 2. Insert or update JobPartCrossRef
        val existingRef = repairJobDao.getJobPart(jobId, inventoryItemId)
        if (existingRef != null) {
            val updatedRef = existingRef.copy(
                quantityUsed = existingRef.quantityUsed + quantity
                // Note: Keep unitPriceAtTimeOfUse unchanged to preserve initial snapshot price
            )
            repairJobDao.updateJobPart(updatedRef)
        } else {
            val newRef = JobPartCrossRef(
                jobId = jobId,
                inventoryItemId = inventoryItemId,
                quantityUsed = quantity,
                unitPriceAtTimeOfUse = item.sellingPrice
            )
            repairJobDao.insertJobPart(newRef)
        }

        // 3. Update Job updatedAt timestamp
        repairJobDao.updateJob(job.copy(updatedAt = System.currentTimeMillis()))

        OperationResult.Success(Unit)
    }

    suspend fun removePartFromJob(
        jobId: Long,
        inventoryItemId: Long
    ): OperationResult<Unit> = database.withTransaction {
        val job = repairJobDao.getJobById(jobId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.JobNotFound)

        if (job.status == JobStatus.DELIVERED || job.status == JobStatus.CANCELLED) {
            return@withTransaction OperationResult.Failure(RepairJobError.JobLocked(job.status.name))
        }

        val crossRef = repairJobDao.getJobPart(jobId, inventoryItemId)
            ?: return@withTransaction OperationResult.Failure(RepairJobError.InventoryItemNotFound)

        val item = inventoryItemDao.getInventoryItemById(inventoryItemId)
        if (item != null) {
            // Restore inventory
            val restoredQuantity = item.quantity + crossRef.quantityUsed
            inventoryItemDao.updateItem(item.copy(quantity = restoredQuantity))
        }

        // Delete job part row
        repairJobDao.deleteJobPart(crossRef)

        // Update Job updatedAt
        repairJobDao.updateJob(job.copy(updatedAt = System.currentTimeMillis()))

        OperationResult.Success(Unit)
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

