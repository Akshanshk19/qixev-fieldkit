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
                        val jobMap = linkedMapOf(
                            "id" to JsonValue.JsonNumber(job.id.toString()),
                            "title" to JsonValue.JsonString(job.displayTitle),
                            "description" to JsonValue.JsonString(job.displayDescription),
                            "customerName" to JsonValue.JsonString(job.customerName),
                            "customerPhone" to JsonValue.JsonString(job.customerPhone),
                            "deviceName" to JsonValue.JsonString(job.deviceName.ifBlank { job.displayTitle }),
                            "problemDescription" to JsonValue.JsonString(job.problemDescription.ifBlank { job.displayDescription }),
                            "status" to JsonValue.JsonString(job.status.name),
                            "estimatedCost" to JsonValue.JsonNumber(job.estimatedCost.toString()),
                            "finalCost" to JsonValue.JsonNumber(job.finalCost.toString()),
                            "createdAt" to JsonValue.JsonNumber(job.createdAt.toString()),
                            "updatedAt" to JsonValue.JsonNumber(job.updatedAt.toString())
                        )
                        JsonValue.JsonObject(jobMap)
                    }
                ),
                "jobParts" to JsonValue.JsonArray(
                    backup.jobParts.map { part ->
                        val partMap = linkedMapOf<String, JsonValue>(
                            "id" to JsonValue.JsonNumber(part.id.toString()),
                            "jobId" to JsonValue.JsonNumber(part.jobId.toString()),
                            "inventoryItemId" to JsonValue.JsonNumber(part.inventoryItemId.toString()),
                            "quantityUsed" to JsonValue.JsonNumber(part.quantityUsed.toString()),
                            "unitPriceAtTimeOfUse" to JsonValue.JsonNumber(part.unitPriceAtTimeOfUse.toString()),
                            "usedAt" to JsonValue.JsonNumber(part.usedAt.toString())
                        )
                        part.location?.let { partMap["location"] = JsonValue.JsonString(it) }
                        part.purpose?.let { partMap["purpose"] = JsonValue.JsonString(it) }
                        JsonValue.JsonObject(partMap)
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
            val rawTitle = obj.getString("title") ?: ""
            val rawDescription = obj.getString("description") ?: ""
            val customerName = obj.getString("customerName") ?: ""
            val customerPhone = obj.getString("customerPhone") ?: ""
            val rawDevice = obj.getString("deviceName") ?: ""
            val rawProblem = obj.getString("problemDescription") ?: ""

            val title = rawTitle.ifBlank { rawDevice }
            val description = rawDescription.ifBlank { rawProblem }
            val deviceName = rawDevice.ifBlank { title }
            val problemDescription = rawProblem.ifBlank { description }

            RepairJobEntity(
                id = obj.getLong("id")
                    ?: throw IllegalArgumentException("repairJobs[$idx] is missing 'id'"),
                title = title,
                description = description,
                customerName = customerName,
                customerPhone = customerPhone,
                deviceName = deviceName,
                problemDescription = problemDescription,
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
                id = obj.getLong("id") ?: 0L,
                jobId = obj.getLong("jobId")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'jobId'"),
                inventoryItemId = obj.getLong("inventoryItemId")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'inventoryItemId'"),
                quantityUsed = obj.getInt("quantityUsed")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'quantityUsed'"),
                unitPriceAtTimeOfUse = obj.getDouble("unitPriceAtTimeOfUse")
                    ?: throw IllegalArgumentException("jobParts[$idx] is missing 'unitPriceAtTimeOfUse'"),
                usedAt = obj.getLong("usedAt") ?: 0L,
                location = obj.getString("location"),
                purpose = obj.getString("purpose")
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

