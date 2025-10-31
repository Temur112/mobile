package zoro.benojir.callrecorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import zoro.benojir.callrecorder.data.CallRecordEntity
import zoro.benojir.callrecorder.databinding.ItemCallListBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CallListAdapter(
    private var callRecords: List<CallRecordEntity>,
    private val onPlayClicked: (File) -> Unit
) : RecyclerView.Adapter<CallListAdapter.CallViewHolder>() {

    inner class CallViewHolder(val binding: ItemCallListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCallListBinding.inflate(inflater, parent, false)
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val record = callRecords[position]
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        holder.binding.apply {
            tvPhoneNumber.text = record.phoneNumber
            tvCallType.text = record.callType.uppercase(Locale.getDefault())
            tvCallStatus.text = record.callStatus
            tvDuration.text = "${record.duration}s"
            tvStartTime.text = formatter.format(Date(record.startTime))
            tvCallId.text = "Call ID: ${record.callId}"

            val fileExists = record.filePath.isNotEmpty() && File(record.filePath).exists()

            if (fileExists) {
                tvFileStatus.text = "🎙️ File Saved"
                root.isEnabled = true
                root.alpha = 1.0f

                // 🟢 Add this UI enhancement snippet here:
                tvFileStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // green

                root.setOnClickListener {
                    onPlayClicked(File(record.filePath))
                }
            } else {
                tvFileStatus.text = "❌ No File"
                root.isEnabled = false
                root.alpha = 0.5f

                // 🔴 Add this part here too:
                tvFileStatus.setTextColor(android.graphics.Color.parseColor("#B0B0B0")) // gray

                root.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = callRecords.size

    fun update(newList: List<CallRecordEntity>) {
        callRecords = newList
        notifyDataSetChanged()
    }
}
