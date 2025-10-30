package zoro.benojir.callrecorder.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import zoro.benojir.callrecorder.data.CallRecordEntity
import zoro.benojir.callrecorder.databinding.ItemCallListBinding
import java.text.SimpleDateFormat
import java.util.*

class CallListAdapter(
    private var callRecords: List<CallRecordEntity>
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

            // 🧭 Call type: inbound/outbound
            tvCallType.text = record.callType.uppercase(Locale.getDefault())

            // 🟢 Status: answered / busy / no_answer / failed
            tvCallStatus.text = record.callStatus

            // ⏱ Duration
            tvDuration.text = "${record.duration}s"

            // 🕓 Start time
            tvStartTime.text = formatter.format(Date(record.startTime))

            // 🎙️ File presence
            tvFileStatus.text = if (record.filePath.isNotEmpty()) "🎙️ File Saved" else "❌ No File"

            tvCallId.text = "Call ID: ${record.callId}"
        }
    }

    override fun getItemCount(): Int = callRecords.size

    fun update(newList: List<CallRecordEntity>) {
        callRecords = newList
        notifyDataSetChanged()
    }
}
