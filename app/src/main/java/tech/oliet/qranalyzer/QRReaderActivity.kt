package tech.oliet.qranalyzer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

internal const val RESULT = "tech.oliet.qranalyzer.RESULT"

class QRReaderActivity : AppCompatActivity() {
    private val qrLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        result.contents ?: return@registerForActivityResult

        setContentView(R.layout.activity_qrreader)

        val myResult = Result(result.contents, result.errorCorrectionLevel, result.rawBytes)

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(RESULT, myResult)
        }

        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val beepEnabled = sp.getBoolean("beep", false)

        val options = ScanOptions().apply {
            setPrompt(getString(R.string.prompt))
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setOrientationLocked(false)
            setBeepEnabled(beepEnabled)
            setCameraId(0)
            captureActivity = ToolbarCaptureActivity::class.java
        }

        qrLauncher.launch(options)
    }
}