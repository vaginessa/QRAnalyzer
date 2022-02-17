package com.example.qranalyzer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

internal const val RESULT_MESSAGE = "com.example.qranalyzer.RESULT_MESSAGE"

class QRReaderActivity : AppCompatActivity() {
    private val qrLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        var resultMessage =
            "${getString(R.string.contents_begin)}\n ${result.contents}\n${getString(R.string.contents_end)}\n\n" +
                    "${getString(R.string.details)}\n" +
                    "${getString(R.string.error_correction_level)}: ${result.errorCorrectionLevel}\n" +
                    "${getString(R.string.raw_data)}: ${result.rawBytes.toHex()}"

        val decoder = SQRCDecoder(result.rawBytes)

        if (decoder.isSQRC()) {
            resultMessage += "\n\n" + getString(R.string.it_is_an_sqrc)

            val decodedContent = decoder.decode()

            resultMessage += "\n\n" + if (decodedContent != null) {
                getString(R.string.decoded_contents) + "\n" + decodedContent
            } else {
                getString(R.string.decode_failed)
            }
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