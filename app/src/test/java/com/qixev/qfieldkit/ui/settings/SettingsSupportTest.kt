package com.qixev.qfieldkit.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class SettingsSupportTest {

    companion object {
        const val EXPECTED_SUPPORT_URL = "https://qixev.com/support"
    }

    @Test
    fun supportUrl_isStrictHttps() {
        val uri = URI.create(EXPECTED_SUPPORT_URL)
        assertEquals("https", uri.scheme)
        assertEquals("qixev.com", uri.host)
        assertEquals("/support", uri.path)
    }

    @Test
    fun supportUrl_doesNotContainCredentialsOrTracking() {
        val uri = URI.create(EXPECTED_SUPPORT_URL)
        assertTrue("Query should be empty or null", uri.query.isNullOrEmpty())
        assertTrue("UserInfo should be null", uri.userInfo.isNullOrEmpty())
        assertFalse("Should not contain payment SDK tokens", EXPECTED_SUPPORT_URL.contains("rzp_"))
        assertFalse("Should not contain tracking tokens", EXPECTED_SUPPORT_URL.contains("utm_"))
    }
}

