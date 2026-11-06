package com.qixev.qfieldkit.data.repository

sealed class RepairJobError(val message: String) {
    object JobNotFound : RepairJobError("Repair job not found.")
    object InventoryItemNotFound : RepairJobError("Inventory item not found.")
    data class InsufficientStock(val available: Int, val requested: Int) :
        RepairJobError("Insufficient stock. Available: $available, requested: $requested.")
    data class InvalidQuantity(val quantity: Int) :
        RepairJobError("Quantity must be greater than 0, got $quantity.")
    data class JobLocked(val status: String) :
        RepairJobError("Job is $status and cannot be modified.")
    data class InvalidStatusTransition(val from: String, val to: String) :
        RepairJobError("Invalid status transition from $from to $to.")
    data class DatabaseError(val detail: String) :
        RepairJobError("Database operation failed: $detail")
}

sealed class OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>()
    data class Failure(val error: RepairJobError) : OperationResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

