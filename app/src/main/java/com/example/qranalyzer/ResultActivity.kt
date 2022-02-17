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

        val result = intent.getStringExtra(RESULT_MESSAGE) ?: return

        vibrate(300)

        val textViewResult = findViewById<TextView>(R.id.textViewResult)

        textViewResult.text = result
    }

    private fun vibrate(milliseconds: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(milliseconds)
    }
}