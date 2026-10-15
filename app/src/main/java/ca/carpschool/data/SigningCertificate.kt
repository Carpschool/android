package ca.carpschool.data

/** Google mobile REST identity requires uppercase SHA-1 hex without delimiters. */
internal fun certificateSha1Header(certificate: ByteArray): String =
    java.security.MessageDigest.getInstance("SHA-1").digest(certificate)
        .joinToString("") { "%02X".format(java.util.Locale.ROOT, it.toInt() and 0xff) }
