package dev.denlogv.lexislearned.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlTest {
    @Test
    fun trailingSlashesSpacesAndTheChatEndpointAreRemoved() {
        assertEquals("https://api.example.com/v1", normalizeBaseUrl("  https://api.example.com/v1/  "))
        assertEquals("https://api.example.com/v1", normalizeBaseUrl("https://api.example.com/v1/chat/completions"))
        assertEquals("https://api.example.com/v1", normalizeBaseUrl("https://api.example.com/v1/chat/completions/"))
    }

    @Test
    fun httpAddressesWithPortsAndPathsAreKept() {
        assertEquals("http://192.168.1.5:11434/v1", normalizeBaseUrl("http://192.168.1.5:11434/v1"))
        assertEquals("http://localhost:1234", normalizeBaseUrl("http://localhost:1234/"))
        assertEquals("HTTPS://Host.example/openai", normalizeBaseUrl("HTTPS://Host.example/openai"))
    }

    @Test
    fun anythingElseIsRejected() {
        listOf(
            "", "   ", "api.example.com/v1", "ftp://host/v1", "https://", "http:///v1",
            "https://h/v1?key=1", "https://h/v1#x", "not a url",
        ).forEach { assertNull(it, normalizeBaseUrl(it)) }
    }
}
