package tech.oliet.qranalyzer

import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import kotlin.concurrent.thread

class ResultActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sp = PreferenceManager.getDefaultSharedPreferences(this)

        val result = (intent.getSerializableExtra(RESULT) ?: return) as Result

        if (sp.getBoolean("vibration", true)) {
            vibrate(300)
        }

        val constraintLayoutResult = findViewById<ConstraintLayout>(R.id.constraintLayoutResult)

        val progressBar = ProgressBar(this)

        constraintLayoutResult.addView(
            progressBar,
            ActionBar.LayoutParams(
                ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT
            )
        )

        thread {
            val resultMessage = getResultMessage(result)

            handler.post {
                val textViewResult = findViewById<TextView>(R.id.textViewResult)
                textViewResult.text = resultMessage

                constraintLayoutResult.removeView(progressBar)
            }
        }

    }

    private fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?)?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)?.vibrate(milliseconds)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuItemSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menuItemLicenses -> {
                val intent = Intent(this, LicensesActivity::class.java)
                startActivity(intent)
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getResultMessage(result: Result): String {

        // contents
        var resultMessage =
            "${getString(R.string.contents_begin)}\n${result.contents}\n${getString(R.string.contents_end)}\n\n"

        try {
            val qrDecoder = QRDecoder(result.rawBytes, result.errorCorrectionLevel)
            val myContents = qrDecoder.decode()

            // details
            resultMessage += "${getString(R.string.details)}\n"

            // is decoding successful
            if (result.contents == myContents) {
                resultMessage += getString(R.string.decoding_succeeded) + "\n"
            }

            // error collection level
            resultMessage +=
                "${getString(R.string.error_correction_level)}: ${result.errorCorrectionLevel} " +
                        "(%d%% ${getString(R.string.restoration_ability)})\n".format(
                            when (result.errorCorrectionLevel) {
                                "L" -> 7
                                "M" -> 15
                                "Q" -> 25
                                "H" -> 30
                                else -> throw RuntimeException("Unknown Error Correction Level")
                            }
                        )

            val cellSize = qrDecoder.calculateCellSize()

            // version
            resultMessage += "${getString(R.string.version)}: ${qrDecoder.version}\n"

            // cell size
            resultMessage += "${getString(R.string.cell_size)}: ${cellSize}x${cellSize}\n"

            // raw data
            resultMessage += "${getString(R.string.raw_data)}: ${result.rawBytes.toHex()}\n"

            // hex contents
            if (qrDecoder.hexContents != "") {
                resultMessage += "\n${getString(R.string.hex_contents)}: ${qrDecoder.hexContents}\n"
            }

            // residual data
            if (qrDecoder.hasResidualData) {
                resultMessage += "\n${getString(R.string.there_is_residual_data)}\n"
                resultMessage += "\n${getString(R.string.residual_data)}: ${qrDecoder.residualData}\n"
            }

            // hidden data
            if (qrDecoder.hasHiddenData) {
                resultMessage += "\n${getString(R.string.there_is_hidden_data)}\n"
                resultMessage += "\n${getString(R.string.hidden_data)}: ${qrDecoder.hiddenData}\n"
            }

            if (!sp.getBoolean("sqrc_enabled", true)) {
                return resultMessage
            }

            val endIndex = qrDecoder.endIndex

            var i = 0

            val regex = Regex("[0-9A-F]{16}", RegexOption.IGNORE_CASE)

            if (SQRCDecoder(result.rawBytes, qrDecoder.version, startIndex = endIndex).isSQRC()) {
                resultMessage += "\n${getString(R.string.it_is_an_sqrc)}\n"

                while (true) {
                    i++
                    val key = sp.getString("key$i", null)
                    // no more keys
                    if (key == null) {
                        resultMessage += "\n" + getString(R.string.decryption_failed) + "\n"
                        break
                    }

                    if (key == "") {
                        continue
                    }

                    if (!regex.containsMatchIn(key)) {
                        val message = getString(R.string.key_is_invalid, i)
                        handler.post {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                        continue
                    }

                    val sqrcDecoder =
                        SQRCDecoder(result.rawBytes, qrDecoder.version, key, startIndex = endIndex)

                    val decodedContents = sqrcDecoder.decode()

                    if (decodedContents != null) {
                        resultMessage += "\n" + getString(R.string.decrypted_contents) + "\n" + decodedContents + "\n"
                        break
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            resultMessage += "\n" + getString(R.string.exception_occurred)
            resultMessage += "\n\n" + e.toString()
        }

        return resultMessage
    }
}