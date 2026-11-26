package com.cintra.app.data.security

/**
 * Hidden, XOR-obfuscated backend gateway configuration.
 * No raw URL or token string literals exist in DEX; protected by R8/ProGuard.
 */
object CintraSecureVault {

    private const val MASK_KEY: Int = 0x5A

    private val WORKER_URL_BYTES: IntArray = intArrayOf(50, 21, 28, 31, 5, 71, 171, 164, 241, 240, 206, 211, 220, 212, 145, 162, 163, 252, 175, 176, 148, 134, 145, 137, 44, 110, 106, 114, 103, 68, 72, 64, 91, 35, 45, 61, 49, 115, 19, 4, 0, 18, 229, 245, 253, 187, 248, 198, 220)
    private val APP_TOKEN_BYTES: IntArray = intArrayOf(57, 15, 28, 48, 26, 20, 242, 238, 205, 160, 198, 147, 229, 141, 209, 241, 155, 230, 160, 143, 211, 155, 166, 202, 108, 69, 38, 96, 68, 22, 78, 123, 10, 34, 17, 119, 34, 23, 81, 64, 84)

    fun resolveEndpoint(): String = decode(WORKER_URL_BYTES).trim().trimEnd('/')

    fun resolveAppToken(): String = decode(APP_TOKEN_BYTES).trim()

    fun isProvisioned(): Boolean = resolveEndpoint().startsWith("https://")

    private fun decode(encoded: IntArray): String {
        if (encoded.isEmpty()) return ""
        val chars = CharArray(encoded.size)
        for (i in encoded.indices) {
            chars[i] = (encoded[i] xor ((MASK_KEY + (i * 7)) and 0xFF)).toChar()
        }
        return String(chars)
    }
}
