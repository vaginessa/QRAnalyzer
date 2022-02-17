package com.example.qranalyzer

import java.security.AlgorithmParameters
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec


private const val IV = "0000000000000000"
private const val DEFAULT_KEY = "0123456789ABCDEF"

internal fun ByteArray.toHex(): String =
    joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

// https://stackoverflow.com/a/66614516
internal fun String.fromHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

class SQRCDecoder(private val rawBytes: ByteArray, private val KEY: String = DEFAULT_KEY) {
    private val rawBytesHex = rawBytes.toHex()

    private val cipher: Cipher = Cipher.getInstance("DES/CBC/NoPadding")

    private val theIV: ByteArray = IV.fromHex()
    private val theKEY: ByteArray = KEY.fromHex()

    init {
        val keyFactory = SecretKeyFactory.getInstance("DES")
        val keySpec = DESKeySpec(theKEY)
        val secretKey = keyFactory.generateSecret(keySpec)
        val iv: AlgorithmParameters = AlgorithmParameters.getInstance("DES")
        iv.init(theIV)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
    }

    fun decode(): String? {
        try {
            val l = lenOfSQRC() ?: return null
            val c = coreOfSQRC(l) ?: return null
            if (c.size % 8 != 0) {
                // Provisional
                val h = c.toHex()
                val cutLen = h.length / 16 * 16
                val d = decrypt(h.substring(0, cutLen).fromHex())
                return "[Residual (Provisional):Incomplete, No Check]\n"+QRDecoder.decode(
                    (d.toHex() + "00".repeat(123)).fromHex()
                ).replace("\u0000", "").dropLast(1) + "?..."
            }

            val d = decrypt(c)
            val h = d.toHex()

            val crc16 = CRC16()
            crc16.update(d, 0, d.size - 2)
            crc16.update(theKEY)

            val swap = { crc: Int -> (crc % 0x100) * 0x100 + (crc / 0x100) }

            if (Integer.parseInt(h.substring(h.length - 4), 16) !=
                swap(crc16.value.toInt())
            ) {
                throw RuntimeException("checksum error")
            }

            return QRDecoder.decode(h.substring(0, h.length - 4).fromHex())
        } catch (e: Exception) {
            return null
        }
    }

    private fun lenOfSQRC(): Int? {
        var i = -1
        while (true) {
            i = rawBytesHex.indexOf("6", i + 1)
            if (i == -1) {
                break
            }

            val j = i + 1

            try {
                val l = Integer.parseInt(rawBytesHex.substring(j, j + 2), 16)
                if (rawBytesHex[j + 2 + l * 2] == '0') {
                    return l
                }
                val ll = Integer.parseInt(rawBytesHex.substring(j, j + 3), 16)
                if (rawBytesHex.length >= j + 2 + ll * 2) {
                    return ll
                }
            } catch (e: StringIndexOutOfBoundsException) {
                /* DO NOTHING */
            }
        }

        return null
    }

    private fun coreOfSQRC(len: Int): ByteArray? {
        check(len in 1..0xFFF)

        val rawRegex = if (len <= 0xFF) {
            "6%02x(.{%d})0".format(len, len * 2)
        } else {
            "6%03x(.{%d})".format(len, len * 2)
        }
        val regex = Regex(rawRegex)
        val find = regex.find(rawBytes.toHex())
        return find?.groupValues?.get(1)?.fromHex()
    }

    private fun decrypt(c: ByteArray): ByteArray {
        check(c.size % 8 == 0)
        return cipher.doFinal(c)
    }

    fun isSQRC(): Boolean {
        return lenOfSQRC() != null
    }
}