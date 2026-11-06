package com.qixev.qfieldkit.ui.jobs

import com.qixev.qfieldkit.data.local.entity.JobStatus
import com.qixev.qfieldkit.data.local.entity.RepairJobEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairJobFilterTest {

    private val sampleJobs = listOf(
        RepairJobEntity(
            id = 1L,
            customerName = "John Doe",
            customerPhone = "+1 555-0101",
            deviceName = "iPhone 13 Pro",
            problemDescription = "Cracked screen",
            status = JobStatus.RECEIVED
        ),
        RepairJobEntity(
            id = 2L,
            customerName = "Sarah Connor",
            customerPhone = "555-0199",
            deviceName = "MacBook Air",
            problemDescription = "Battery service required",
            status = JobStatus.IN_PROGRESS
        ),
        RepairJobEntity(
            id = 3L,
            customerName = "Mike Johnson",
            customerPhone = "555-0144",
            deviceName = "Google Pixel 7",
            problemDescription = "No power after drop",
            status = JobStatus.COMPLETED
        ),
        RepairJobEntity(
            id = 4L,
            customerName = "Alice Williams",
            customerPhone = "555-0177",
            deviceName = "iPhone 14",
            problemDescription = "Water damage",
            status = JobStatus.DELIVERED
        ),
        RepairJobEntity(
            id = 5L,
            customerName = "Bob Vance",
            customerPhone = "555-0122",
            deviceName = "iPad Air",
            problemDescription = "Declined estimate",
            status = JobStatus.CANCELLED
        )
    )

    @Test
    fun filterByCustomerName_matchesCaseInsensitive() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "sarah", selectedStatus = null)
        assertEquals(1, result.size)
        assertEquals("Sarah Connor", result[0].customerName)
    }

    @Test
    fun filterByPhone_matchesPhoneSubstring() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "0144", selectedStatus = null)
        assertEquals(1, result.size)
        assertEquals("Mike Johnson", result[0].customerName)
    }

    @Test
    fun filterByDeviceName_matchesDevice() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "iphone", selectedStatus = null)
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 4L })
    }

    @Test
    fun filterByJobId_matchesNumberPrefix() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "#3", selectedStatus = null)
        assertEquals(1, result.size)
        assertEquals(3L, result[0].id)
    }

    @Test
    fun filterByStatus_returnsOnlySpecifiedStatus() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "", selectedStatus = JobStatus.RECEIVED)
        assertEquals(1, result.size)
        assertEquals(JobStatus.RECEIVED, result[0].status)
    }

    @Test
    fun filterBySearchAndStatus_returnsIntersection() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "iphone", selectedStatus = JobStatus.RECEIVED)
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)

        val emptyResult = RepairJobFilter.filter(sampleJobs, searchQuery = "macbook", selectedStatus = JobStatus.RECEIVED)
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun filterWithEmptyQueryAndNullStatus_returnsAll() {
        val result = RepairJobFilter.filter(sampleJobs, searchQuery = "", selectedStatus = null)
        assertEquals(5, result.size)
    }
}

