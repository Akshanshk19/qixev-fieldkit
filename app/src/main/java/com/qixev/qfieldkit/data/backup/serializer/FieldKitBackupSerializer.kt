package com.qixev.qfieldkit.data.backup.serializer

import com.qixev.qfieldkit.data.backup.json.FieldKitJson
import com.qixev.qfieldkit.data.backup.json.JsonValue
import com.qixev.qfieldkit.data.backup.model.FieldKitBackup
import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef
import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity

object FieldKitBackupSerializer {

    fun serialize(backup: FieldKitBackup): String {
        val root = JsonValue.JsonObject(
            linkedMapOf(
                "backupVersion" to JsonValue.JsonNumber(backup.backupVersion.toString()),
                "exportedAt" to JsonValue.JsonString(backup.exportedAt),
                "inventoryItems" to JsonValue.JsonArray(
                    backup.inventoryItems.map { item ->
                        JsonValue.JsonObject(
                            linkedMapOf(
                                "id" to JsonValue.JsonNumber(item.id.toString()),
                                "name" to JsonValue.JsonString(item.name),
                                "sku" to JsonValue.JsonString(item.sku),
                                "category" to JsonValue.JsonString(item.category),
                                "quantity" to JsonValue.JsonNumber(item.quantity.toString()),
                                "minimumStock" to JsonValue.JsonNumber(item.minimumStock.toString()),
                                "purchasePrice" to JsonValue.JsonNumber(item.purchasePrice.toString()),
                                "sellingPrice" to JsonValue.JsonNumber(item.sellingPrice.toString())
                            )
                        )
                    }
                ),
                "repairJobs" to JsonValue.JsonArray(
                    backup.repairJobs.map { job ->
                        JsonValue.JsonObject(
                            linkedMapOf(
                                "id" to JsonValue.JsonNumber(job.id.toString()),
                                "customerName" to JsonValue.JsonString(job.customerName),
                                "customerPhone" to JsonValue.JsonString(job.customerPhone),
                                "deviceName" to JsonValue.JsonString(job.deviceName),
                                "problemDescription" to JsonValue.JsonString(job.problemDescription),
                                "status" to JsonValue.JsonString(job.status.name),
                                "estimatedCost" to JsonValue.JsonNumber(job.estimatedCost.toString()),
                                "finalCost" to JsonValue.JsonNumber(job.finalCost.toString()),
                                "createdAt" to JsonValue.JsonNumber(job.createdAt.toString()),
                                "updatedAt" to JsonValue.JsonNumber(job.updatedAt.toString())
                            )
                        )
                    }
                ),
                "jobParts" to JsonValue.JsonArray(
                    backup.jobParts.map { part ->
                        JsonValue.JsonObject(
                            linkedMapOf(
                                "jobId" to JsonValue.JsonNumber(part.jobId.toString()),
                                "inventoryItemId" to JsonValue.JsonNumber(part.inventoryItemId.toString()),
                                "quantityUsed" to JsonValue.JsonNumber(part.quantityUsed.toString()),
                                "unitPriceAtTimeOfUse" to JsonValue.JsonNumber(part.unitPriceAtTimeOfUse.toString())
                            )
                        )
                    }
                )
            )
        )
        return FieldKitJson.stringify(root)
    }

    fun deserialize(root: JsonValue.JsonObject): FieldKitBackup {
        val version = root.getInt("backupVersion")
            ?: throw IllegalArgumentException("Missing or invalid 'backupVersion'")
        val exportedAt = root.getString("exportedAt")
            ?: throw IllegalArgumentException("Missing or invalid 'exportedAt'")

        val inventoryArray = root.getArray("inventoryItems")
            ?: throw IllegalArgumentException("Missing or invalid 'inventoryItems' array")
        val inventoryItems = inventoryArray.elements.mapIndexed { idx, el ->
            val obj = el as? JsonValue.JsonObject
                ?: throw IllegalArgumentException("inventoryItems[$idx] must be an object")
            InventoryItemEntity(
                id = obj.getLong("id")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'id'"),
                name = obj.getString("name")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'name'"),
                sku = obj.getString("sku")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'sku'"),
                category = obj.getString("category")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'category'"),
                quantity = obj.getInt("quantity")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'quantity'"),
                minimumStock = obj.getInt("minimumStock")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'minimumStock'"),
                purchasePrice = obj.getDouble("purchasePrice")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'purchasePrice'"),
                sellingPrice = obj.getDouble("sellingPrice")
                    ?: throw IllegalArgumentException("inventoryItems[$idx] is missing 'sellingPrice'")
            )
        }

        val jobsArray = root.getArray("repairJobs")
            ?: throw IllegalArgumentException("Missing or invalid 'repairJobs' array")
        val repairJobs = jobsArray.elements.mapIndexed { idx, el ->
            val obj = el as? JsonValue.JsonObject
                ?: throw IllegalArgumentException("repairJobs[$idx] must be an object")
            val statusStr = obj.getString("status")
                ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'status'")
            val status = try {
                JobStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("repairJobs[$idx] has invalid status '$statusStr'")
            }
            RepairJobEntity(
                id = obj.getLong("id")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'id'"),
                customerName = obj.getString("customerName")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'customerName'"),
                customerPhone = obj.getString("customerPhone")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'customerPhone'"),
                deviceName = obj.getString("deviceName")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'deviceName'"),
                problemDescription = obj.getString("problemDescription")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'problemDescription'"),
                status = status,
                estimatedCost = obj.getDouble("estimatedCost") ?: 0.0,
                finalCost = obj.getDouble("finalCost") ?: 0.0,
                createdAt = obj.getLong("createdAt")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'createdAt'"),
                updatedAt = obj.getLong("updatedAt")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'updatedAt'")
            )
        }

        val partsArray = root.getArray("jobParts")
            ?: throw IllegalArgumentException("Missing or invalid 'jobParts' array")
        val jobParts = partsArray.elements.mapIndexed { idx, el ->
            val obj = el as? JsonValue.JsonObject
                ?: throw IllegalArgumentException("jobParts[$idx] must be an object")
            JobPartCrossRef(
                jobId = obj.getLong("jobId")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'jobId'"),
                inventoryItemId = obj.getLong("inventoryItemId")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'inventoryItemId'"),
                quantityUsed = obj.getInt("quantityUsed")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'quantityUsed'"),
                unitPriceAtTimeOfUse = obj.getDouble("unitPriceAtTimeOfUse")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'unitPriceAtTimeOfUse'")
            )
        }

        return FieldKitBackup(
            backupVersion = version,
            exportedAt = exportedAt,
            inventoryItems = inventoryItems,
            repairJobs = repairJobs,
            jobParts = jobParts
        )
    }
}

