package com.qixev.qfieldkit.data.backup.validator

import com.qixev.qfieldkit.data.backup.json.FieldKitJson
import com.qixev.qfieldkit.data.backup.json.JsonParseException
import com.qixev.qfieldkit.data.backup.json.JsonValue
import com.qixev.qfieldkit.data.backup.model.BackupValidationResult
import com.qixev.qfieldkit.data.backup.model.FieldKitBackup
import com.qixev.qfieldkit.data.backup.serializer.FieldKitBackupSerializer

object BackupValidator {

    fun validate(jsonString: String): BackupValidationResult {
        if (jsonString.isBlank()) {
            return BackupValidationResult.Error("Backup file is empty.")
        }

        val jsonValue = try {
            FieldKitJson.parse(jsonString)
        } catch (e: JsonParseException) {
            return BackupValidationResult.Error("Invalid JSON format: ${e.message}")
        } catch (e: Exception) {
            return BackupValidationResult.Error("Failed to parse JSON: ${e.message ?: "Malformed file"}")
        }

        val root = jsonValue as? JsonValue.JsonObject
            ?: return BackupValidationResult.Error("Root of backup must be a JSON object.")

        val version = root.getInt("backupVersion")
            ?: return BackupValidationResult.Error("Missing or invalid 'backupVersion' in backup header.")

        if (version != FieldKitBackup.CURRENT_VERSION) {
            return BackupValidationResult.Error(
                "Unsupported backup version $version. This app version supports version ${FieldKitBackup.CURRENT_VERSION}."
            )
        }

        val backup = try {
            FieldKitBackupSerializer.deserialize(root)
        } catch (e: Exception) {
            return BackupValidationResult.Error("Malformed backup structure: ${e.message}")
        }

        // Validate Inventory Items
        val seenItemIds = mutableSetOf<Long>()
        val seenSkus = mutableSetOf<String>()

        for (item in backup.inventoryItems) {
            if (item.id <= 0) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has invalid ID (${item.id}). IDs must be positive.")
            }
            if (!seenItemIds.add(item.id)) {
                return BackupValidationResult.Error("Duplicate inventory item ID: ${item.id}.")
            }
            if (item.name.isBlank()) {
                return BackupValidationResult.Error("Inventory item (ID: ${item.id}) has a blank name.")
            }
            if (item.sku.isBlank()) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has a blank SKU.")
            }
            val normalizedSku = item.sku.trim()
            if (!seenSkus.add(normalizedSku)) {
                return BackupValidationResult.Error("Duplicate SKU '$normalizedSku' found in inventory items.")
            }
            if (item.quantity < 0) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has negative quantity (${item.quantity}).")
            }
            if (item.minimumStock < 0) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has negative minimum stock (${item.minimumStock}).")
            }
            if (item.purchasePrice < 0.0) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has negative purchase price (${item.purchasePrice}).")
            }
            if (item.sellingPrice < 0.0) {
                return BackupValidationResult.Error("Inventory item '${item.name}' has negative selling price (${item.sellingPrice}).")
            }
        }

        // Validate Repair Jobs
        val seenJobIds = mutableSetOf<Long>()

        for (job in backup.repairJobs) {
            if (job.id <= 0) {
                return BackupValidationResult.Error("Repair job has invalid ID (${job.id}). IDs must be positive.")
            }
            if (!seenJobIds.add(job.id)) {
                return BackupValidationResult.Error("Duplicate repair job ID: ${job.id}.")
            }
            if (job.customerName.isBlank()) {
                return BackupValidationResult.Error("Repair job #${job.id} has a blank customer name.")
            }
            if (job.customerPhone.isBlank()) {
                return BackupValidationResult.Error("Repair job #${job.id} has a blank customer phone.")
            }
            if (job.deviceName.isBlank()) {
                return BackupValidationResult.Error("Repair job #${job.id} has a blank device name.")
            }
            if (job.problemDescription.isBlank()) {
                return BackupValidationResult.Error("Repair job #${job.id} has a blank problem description.")
            }
            if (job.estimatedCost < 0.0) {
                return BackupValidationResult.Error("Repair job #${job.id} has negative estimated cost (${job.estimatedCost}).")
            }
            if (job.finalCost < 0.0) {
                return BackupValidationResult.Error("Repair job #${job.id} has negative final cost (${job.finalCost}).")
            }
            if (job.createdAt <= 0) {
                return BackupValidationResult.Error("Repair job #${job.id} has invalid createdAt timestamp.")
            }
            if (job.updatedAt <= 0) {
                return BackupValidationResult.Error("Repair job #${job.id} has invalid updatedAt timestamp.")
            }
        }

        // Validate Job Parts & Referential Integrity
        val seenPartKeys = mutableSetOf<Pair<Long, Long>>()

        for (part in backup.jobParts) {
            if (!seenJobIds.contains(part.jobId)) {
                return BackupValidationResult.Error(
                    "Job part references unknown repair job ID ${part.jobId}. Referential integrity violated."
                )
            }
            if (!seenItemIds.contains(part.inventoryItemId)) {
                return BackupValidationResult.Error(
                    "Job part for job #${part.jobId} references unknown inventory item ID ${part.inventoryItemId}. Referential integrity violated."
                )
            }
            val key = Pair(part.jobId, part.inventoryItemId)
            if (!seenPartKeys.add(key)) {
                return BackupValidationResult.Error(
                    "Duplicate part record for job #${part.jobId} and item #${part.inventoryItemId}."
                )
            }
            if (part.quantityUsed <= 0) {
                return BackupValidationResult.Error(
                    "Job #${part.jobId} part allocation must have quantity > 0 (found ${part.quantityUsed})."
                )
            }
            if (part.unitPriceAtTimeOfUse < 0.0) {
                return BackupValidationResult.Error(
                    "Job #${part.jobId} part has negative unit price (${part.unitPriceAtTimeOfUse})."
                )
            }
        }

        return BackupValidationResult.Success(backup)
    }
}

