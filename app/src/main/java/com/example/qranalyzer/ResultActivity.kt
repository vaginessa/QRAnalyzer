package com.example.qranalyzer

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val result: String? = intent.getStringExtra(RESULT_MESSAGE)

        val textViewResult = findViewById<TextView>(R.id.textViewResult)

        if (result != null) {
            vibrate(300)
            textViewResult.text = result
        } else {
            textViewResult.text = getString(R.string.message_null_result_error)
        }
    }

    private fun vibrate(milliseconds: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(milliseconds)
    }
}