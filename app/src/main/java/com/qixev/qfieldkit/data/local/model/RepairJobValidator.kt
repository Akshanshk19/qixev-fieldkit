package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.JobPartCrossRef

data class RepairJobValidationErrors(
    val customerNameError: String? = null,
    val customerPhoneError: String? = null,
    val deviceNameError: String? = null,
    val problemDescriptionError: String? = null,
    val estimatedCostError: String? = null,
    val finalCostError: String? = null
) {
    val hasErrors: Boolean
        get() = customerNameError != null ||
                customerPhoneError != null ||
                deviceNameError != null ||
                problemDescriptionError != null ||
                estimatedCostError != null ||
                finalCostError != null
}

object RepairJobValidator {

    data class ValidationResult(
        val errors: RepairJobValidationErrors,
        val parsedEstimatedCost: Double? = null,
        val parsedFinalCost: Double? = null
    ) {
        val isValid: Boolean get() = !errors.hasErrors
    }

    fun validate(
        customerName: String,
        customerPhone: String,
        deviceName: String,
        problemDescription: String,
        estimatedCostStr: String,
        finalCostStr: String
    ): ValidationResult {
        var customerNameError: String? = null
        var customerPhoneError: String? = null
        var deviceNameError: String? = null
        var problemDescriptionError: String? = null
        var estimatedCostError: String? = null
        var finalCostError: String? = null

        if (customerName.isBlank()) {
            customerNameError = "Customer name cannot be blank"
        }

        if (customerPhone.isBlank()) {
            customerPhoneError = "Customer phone cannot be blank"
        }

        if (deviceName.isBlank()) {
            deviceNameError = "Device name cannot be blank"
        }

        if (problemDescription.isBlank()) {
            problemDescriptionError = "Problem description cannot be blank"
        }

        val parsedEstimatedCost = estimatedCostStr.toDoubleOrNull()
        if (parsedEstimatedCost == null) {
            estimatedCostError = "Estimated cost must be a valid number"
        } else if (parsedEstimatedCost < 0.0) {
            estimatedCostError = "Estimated cost cannot be negative"
        }

        val parsedFinalCost = finalCostStr.toDoubleOrNull()
        if (parsedFinalCost == null) {
            finalCostError = "Final cost must be a valid number"
        } else if (parsedFinalCost < 0.0) {
            finalCostError = "Final cost cannot be negative"
        }

        val errors = RepairJobValidationErrors(
            customerNameError = customerNameError,
            customerPhoneError = customerPhoneError,
            deviceNameError = deviceNameError,
            problemDescriptionError = problemDescriptionError,
            estimatedCostError = estimatedCostError,
            finalCostError = finalCostError
        )

        return ValidationResult(
            errors = errors,
            parsedEstimatedCost = parsedEstimatedCost,
            parsedFinalCost = parsedFinalCost
        )
    }

    fun validateWorkUnit(
        title: String,
        description: String = "",
        estimatedCostStr: String = "0.0"
    ): ValidationResult {
        var titleError: String? = null
        var estimatedCostError: String? = null

        if (title.isBlank()) {
            titleError = "Title cannot be blank"
        }

        val parsedEstimatedCost = estimatedCostStr.toDoubleOrNull()
        if (parsedEstimatedCost == null) {
            estimatedCostError = "Estimated cost must be a valid number"
        } else if (parsedEstimatedCost < 0.0) {
            estimatedCostError = "Estimated cost cannot be negative"
        }

        val errors = RepairJobValidationErrors(
            deviceNameError = titleError,
            estimatedCostError = estimatedCostError
        )

        return ValidationResult(
            errors = errors,
            parsedEstimatedCost = parsedEstimatedCost,
            parsedFinalCost = 0.0
        )
    }

    fun calculatePartsTotal(parts: List<JobPartCrossRef>): Double {
        return parts.sumOf { it.quantityUsed * it.unitPriceAtTimeOfUse }
    }
}

