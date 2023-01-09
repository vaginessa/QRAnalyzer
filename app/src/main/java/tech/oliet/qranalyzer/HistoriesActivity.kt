package tech.oliet.qranalyzer

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HistoriesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_histories)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val linearLayoutHistory = findViewById<LinearLayout>(R.id.linearLayoutHistory)
        val textView1 = TextView(this)
        textView1.text = getString(R.string.not_yet_implemented)
        linearLayoutHistory.addView(textView1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}