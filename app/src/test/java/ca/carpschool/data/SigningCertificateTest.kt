package ca.carpschool.data

import org.junit.Assert.*
import org.junit.Test

class SigningCertificateTest {
    @Test fun knownDigestIsDelimiterFreeUppercaseSha1() {
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", certificateSha1Header("abc".toByteArray()))
    }
    @Test fun binaryCertificateProducesExactlyFortyHexCharacters() {
        val header = certificateSha1Header(byteArrayOf(0, 127, -128, -1))
        assertTrue(header.matches(Regex("[0-9A-F]{40}")))
        assertFalse(header.contains(":"))
    }
    @Test fun routesRequestIncludesSigningCertificateHeader() {
        val digest = certificateSha1Header("abc".toByteArray())
        val request = routesRequest("{}", "synthetic-test-key", "ca.carpschool", digest)
        assertEquals(digest, request.header("X-Android-Cert"))
        assertEquals("ca.carpschool", request.header("X-Android-Package"))
        assertEquals("synthetic-test-key", request.header("X-Goog-Api-Key"))
        assertEquals("POST", request.method)
        assertEquals("https://routes.googleapis.com/directions/v2:computeRoutes", request.url.toString())
    }
}
