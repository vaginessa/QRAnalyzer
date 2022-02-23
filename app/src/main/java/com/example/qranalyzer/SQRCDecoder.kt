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

class SQRCDecoder(
    private val rawBytes: ByteArray,
    private val version: Int,
    private val KEY: String = DEFAULT_KEY,
    private val startIndex: Int = 0
) {
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
        val ls = lensOfSQRC()
        for ((len, index) in ls) {
            val c = coreOfSQRC(len, index)

            if (c.size % 8 != 0) {
                // Provisional
                continue
//                val h = c.toHex()
//                val cutLen = h.length / 16 * 16
//                val d = decrypt(h.substring(0, cutLen).fromHex())
//                return "[Residual (Provisional):Incomplete, No Check]\n" +
//                        QRDecoder((d.toHex() + "00".repeat(123)).fromHex(), version).decode()
//                            .replace("\u0000", "").dropLast(1) + "?..."
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
//                    throw RuntimeException("checksum error")
                // checksum error
                continue
            }

            return QRDecoder(h.substring(0, h.length - 4).fromHex(), version).decode()
        }
        return null
    }

    private fun lensOfSQRC(): Array<Pair<Int, Int>> {
        var lens = emptyArray<Pair<Int, Int>>()

        var i = startIndex - 1
        do {
            i = rawBytesHex.indexOf("6", i + 1)

            val j = i + 1

            try {
                val l = Integer.parseInt(rawBytesHex.substring(j, j + 2), 16)
                if (rawBytesHex[j + 2 + l * 2] == '0') {
                    lens += l * 2 to j + 2
                }
                val ll = Integer.parseInt(rawBytesHex.substring(j, j + 3), 16)
                if (rawBytesHex.length >= j + 2 + ll * 2) {
                    lens += ll * 2 to j + 3
                }
            } catch (e: StringIndexOutOfBoundsException) {
                /* DO NOTHING */
            }
        } while (i != -1)

        return lens
    }

    private fun coreOfSQRC(len: Int, index: Int): ByteArray {
        check(len in 1..0xFFF)

        return rawBytesHex.slice(index until index + len).fromHex()
    }

    private fun decrypt(c: ByteArray): ByteArray {
        check(c.size % 8 == 0)
        return cipher.doFinal(c)
    }

    fun isSQRC(): Boolean {
        return lensOfSQRC().isNotEmpty()
    }
}