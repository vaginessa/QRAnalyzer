package com.example.qranalyzer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

internal const val RESULT_MESSAGE = "com.example.qranalyzer.RESULT_MESSAGE"

class QRReaderActivity : AppCompatActivity() {
    private val qrLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        result.contents ?: return@registerForActivityResult

        // contents
        var resultMessage =
            "${getString(R.string.contents_begin)}\n${result.contents}\n${getString(R.string.contents_end)}\n\n"

        try {
            val qrDecoder = QRDecoder(result.rawBytes, result.errorCorrectionLevel)
            val myContents = qrDecoder.decode()

            // details
            resultMessage += "${getString(R.string.details)}\n"

            // is decoding successful
            resultMessage += if (result.contents == myContents) {
                getString(R.string.decode_success)
            } else {
                getString(R.string.decode_failed)
            } + "\n"

            // error collection level
            resultMessage += "${getString(R.string.error_correction_level)}: ${result.errorCorrectionLevel} (" +
                    "%d%% ".format(
                        when (result.errorCorrectionLevel) {
                            "L" -> 7
                            "M" -> 15
                            "Q" -> 25
                            "H" -> 30
                            else -> -1
                        }
                    ) + getString(R.string.restoration_ability) + ")\n"

            val cellSize = qrDecoder.calculateCellSize()

            // cell size
            resultMessage += "${getString(R.string.version)}: ${qrDecoder.version}\n" +
                    "${getString(R.string.cell_size)}: ${cellSize}x${cellSize}\n"

            // raw data
            resultMessage += "${getString(R.string.raw_data)}: ${result.rawBytes.toHex()}\n"

            // hex contents
            if (qrDecoder.hexContents != "") {
                resultMessage += "\n${getString(R.string.hex_contents)}: ${qrDecoder.hexContents}\n"
            }

        } catch (e: Exception) {
            resultMessage += e.printStackTrace()
        }

        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(RESULT_MESSAGE, resultMessage)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setOrientationLocked(false)
        options.setBeepEnabled(false)

        qrLauncher.launch(options)
    }
}