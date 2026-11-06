package com.qixev.qfieldkit.data.local.model

import com.qixev.qfieldkit.data.local.entity.JobStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobStatusTransitionsTest {

    @Test
    fun validForwardTransitions_accepted() {
        // RECEIVED -> IN_PROGRESS
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.RECEIVED, JobStatus.IN_PROGRESS))

        // IN_PROGRESS -> WAITING_FOR_PARTS
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.IN_PROGRESS, JobStatus.WAITING_FOR_PARTS))

        // WAITING_FOR_PARTS -> IN_PROGRESS
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.WAITING_FOR_PARTS, JobStatus.IN_PROGRESS))

        // IN_PROGRESS -> COMPLETED
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.IN_PROGRESS, JobStatus.COMPLETED))

        // COMPLETED -> DELIVERED
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.COMPLETED, JobStatus.DELIVERED))
    }

    @Test
    fun cancellation_allowedFromNonDeliveredStates() {
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.RECEIVED, JobStatus.CANCELLED))
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.IN_PROGRESS, JobStatus.CANCELLED))
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.WAITING_FOR_PARTS, JobStatus.CANCELLED))
        assertTrue(JobStatusTransitions.isValidTransition(JobStatus.COMPLETED, JobStatus.CANCELLED))
    }

    @Test
    fun sameState_isAllowed() {
        JobStatus.entries.forEach { status ->
            assertTrue(JobStatusTransitions.isValidTransition(status, status))
        }
    }

    @Test
    fun delivered_isTerminal_noTransitionsAllowed() {
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.DELIVERED, JobStatus.IN_PROGRESS))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.DELIVERED, JobStatus.RECEIVED))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.DELIVERED, JobStatus.COMPLETED))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.DELIVERED, JobStatus.WAITING_FOR_PARTS))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.DELIVERED, JobStatus.CANCELLED))
    }

    @Test
    fun cancelled_isTerminal_noTransitionsAllowed() {
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.CANCELLED, JobStatus.RECEIVED))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.CANCELLED, JobStatus.IN_PROGRESS))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.CANCELLED, JobStatus.WAITING_FOR_PARTS))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.CANCELLED, JobStatus.COMPLETED))
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.CANCELLED, JobStatus.DELIVERED))
    }

    @Test
    fun invalidSkips_rejected() {
        // Cannot jump directly from RECEIVED to DELIVERED
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.RECEIVED, JobStatus.DELIVERED))

        // Cannot jump directly from RECEIVED to COMPLETED
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.RECEIVED, JobStatus.COMPLETED))

        // Cannot jump directly from WAITING_FOR_PARTS to DELIVERED
        assertFalse(JobStatusTransitions.isValidTransition(JobStatus.WAITING_FOR_PARTS, JobStatus.DELIVERED))
    }
}

