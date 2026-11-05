package com.qixev.qfieldkit.ui.inventory

object InventoryValidator {

    data class ValidationResult(
        val errors: InventoryFormValidationErrors,
        val parsedQuantity: Int? = null,
        val parsedMinimumStock: Int? = null,
        val parsedPurchasePrice: Double? = null,
        val parsedSellingPrice: Double? = null
    ) {
        val isValid: Boolean get() = !errors.hasErrors
    }

    fun validate(
        name: String,
        sku: String,
        quantityStr: String,
        minimumStockStr: String,
        purchasePriceStr: String,
        sellingPriceStr: String
    ): ValidationResult {
        var nameError: String? = null
        var skuError: String? = null
        var quantityError: String? = null
        var minimumStockError: String? = null
        var purchasePriceError: String? = null
        var sellingPriceError: String? = null

        if (name.isBlank()) {
            nameError = "Name cannot be blank"
        }

        if (sku.isBlank()) {
            skuError = "SKU cannot be blank"
        }

        val parsedQuantity = quantityStr.toIntOrNull()
        if (parsedQuantity == null) {
            quantityError = "Quantity must be a valid integer"
        } else if (parsedQuantity < 0) {
            quantityError = "Quantity cannot be negative"
        }

        val parsedMinimumStock = minimumStockStr.toIntOrNull()
        if (parsedMinimumStock == null) {
            minimumStockError = "Minimum stock must be a valid integer"
        } else if (parsedMinimumStock < 0) {
            minimumStockError = "Minimum stock cannot be negative"
        }

        val parsedPurchasePrice = purchasePriceStr.toDoubleOrNull()
        if (parsedPurchasePrice == null) {
            purchasePriceError = "Purchase price must be a valid number"
        } else if (parsedPurchasePrice < 0.0) {
            purchasePriceError = "Purchase price cannot be negative"
        }

        val parsedSellingPrice = sellingPriceStr.toDoubleOrNull()
        if (parsedSellingPrice == null) {
            sellingPriceError = "Selling price must be a valid number"
        } else if (parsedSellingPrice < 0.0) {
            sellingPriceError = "Selling price cannot be negative"
        }

        val errors = InventoryFormValidationErrors(
            nameError = nameError,
            skuError = skuError,
            quantityError = quantityError,
            minimumStockError = minimumStockError,
            purchasePriceError = purchasePriceError,
            sellingPriceError = sellingPriceError
        )

        return ValidationResult(
            errors = errors,
            parsedQuantity = parsedQuantity,
            parsedMinimumStock = parsedMinimumStock,
            parsedPurchasePrice = parsedPurchasePrice,
            parsedSellingPrice = parsedSellingPrice
        )
    }
}

