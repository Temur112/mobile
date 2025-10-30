package zoro.benojir.callrecorder.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import zoro.benojir.callrecorder.adapters.CallListAdapter
import zoro.benojir.callrecorder.data.CallListViewModel
import zoro.benojir.callrecorder.databinding.ActivityCallListBinding

class CallListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallListBinding
    private val viewModel: CallListViewModel by viewModels()
    private lateinit var adapter: CallListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Handle the toolbar back arrow
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 🔹 Setup RecyclerView
        adapter = CallListAdapter(emptyList())
        binding.recyclerViewCalls.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewCalls.adapter = adapter

        lifecycleScope.launch {
            viewModel.callRecords.collectLatest {
                adapter.update(it)
            }
        }

        viewModel.loadAllRecords()
    }

    // 🔹 Handle physical Back button press
    override fun onBackPressed() {
        super.onBackPressed()
        finish()  // ensures smooth exit to previous screen
    }
}
