package com.qixev.qfieldkit.data.backup.csv

import com.qixev.qfieldkit.data.local.entity.InventoryItemEntity
import java.util.Locale

object InventoryCsvExporter {

    private const val HEADER = "ID,Name,SKU,Category,Quantity,MinimumStock,PurchasePrice,SellingPrice"

    fun export(items: List<InventoryItemEntity>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\r\n")

        for (item in items) {
            sb.append(item.id).append(",")
            sb.append(escapeCsv(item.name)).append(",")
            sb.append(escapeCsv(item.sku)).append(",")
            sb.append(escapeCsv(item.category)).append(",")
            sb.append(item.quantity).append(",")
            sb.append(item.minimumStock).append(",")
            sb.append(String.format(Locale.US, "%.2f", item.purchasePrice)).append(",")
            sb.append(String.format(Locale.US, "%.2f", item.sellingPrice))
            sb.append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')

        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}

