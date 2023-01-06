package tech.oliet.qranalyzer

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import tech.oliet.qranalyzer.databinding.ActivityLicensesBinding

class LicensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, getString(R.string.not_yet_implemented), Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}