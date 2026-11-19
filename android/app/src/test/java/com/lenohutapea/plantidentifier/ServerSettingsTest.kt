package com.lenohutapea.plantidentifier

import com.lenohutapea.plantidentifier.data.normalizeBaseUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerSettingsTest {
    @Test
    fun addsSchemeAndTrailingSlash() {
        assertEquals("http://plant-api.10.110.231.11.nip.io/", normalizeBaseUrl("plant-api.10.110.231.11.nip.io"))
        assertEquals("http://plant-api.10.110.231.11.nip.io/", normalizeBaseUrl("  http://plant-api.10.110.231.11.nip.io  "))
    }

    @Test
    fun keepsHttpsAndPort() {
        assertEquals("https://example.com:8443/api/", normalizeBaseUrl("https://example.com:8443/api"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(normalizeBaseUrl(""))
        assertNull(normalizeBaseUrl("http://"))
        assertNull(normalizeBaseUrl("not a url with spaces"))
    }
}
