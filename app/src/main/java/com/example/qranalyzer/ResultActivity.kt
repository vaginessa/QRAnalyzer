package com.example.qranalyzer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val result: String? = intent.getStringExtra(RESULT_MESSAGE)

        val textViewResult = findViewById<TextView>(R.id.textViewResult)

        if (result != null) {
            textViewResult.text = result
        } else {
            textViewResult.text = getString(R.string.message_null_result_error)
        }
    }
}