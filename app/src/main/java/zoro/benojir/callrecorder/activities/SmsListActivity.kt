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

        recyclerView = findViewById(R.id.recyclerView)
        adapter = SmsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe data
        lifecycleScope.launch {
            viewModel.smsList.collect { list ->
                adapter.submitList(list)
            }
        }

        // Load initial data
        viewModel.loadSms()
    }
}
