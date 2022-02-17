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
        val resultMessage = "${getString(R.string.content)}: ${result.contents}\n\n" +
                "${getString(R.string.hex)}: ${result.rawBytes.toHex()}"

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