package zoro.benojir.callrecorder.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import zoro.benojir.callrecorder.R
import zoro.benojir.callrecorder.adapters.SmsAdapter
import zoro.benojir.callrecorder.viewmodel.SmsViewModel

class SmsListActivity : AppCompatActivity() {

    private val viewModel: SmsViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SmsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_list)

        // ✅ Setup toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // ✅ Enable back arrow
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "SMS Messages"

        recyclerView = findViewById(R.id.smsRecyclerView)
        adapter = SmsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.loadSms()

        lifecycleScope.launch {
            viewModel.smsList.collect { smsList ->
                android.util.Log.d("SmsListActivity", "Loaded ${smsList.size} messages")
                adapter.submitList(smsList)
            }
        }
    }

    // ✅ Handle back arrow press
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
