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
}
