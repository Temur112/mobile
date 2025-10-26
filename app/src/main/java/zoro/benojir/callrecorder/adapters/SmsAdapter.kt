package zoro.benojir.callrecorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import zoro.benojir.callrecorder.R
import zoro.benojir.callrecorder.data.SmsEntity
import java.text.SimpleDateFormat
import java.util.*

class SmsAdapter : ListAdapter<SmsEntity, SmsAdapter.SmsViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderText: TextView = itemView.findViewById(R.id.senderTextView)
        private val receiverText: TextView = itemView.findViewById(R.id.receiverTextView)
        private val bodyText: TextView = itemView.findViewById(R.id.bodyTextView)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampTextView)
        private val syncStatusText: TextView = itemView.findViewById(R.id.syncStatusTextView)

        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(sms: SmsEntity) {
            senderText.text = "From: ${sms.sender}"
            receiverText.text = "To: ${sms.receiver}"
            bodyText.text = sms.text
            timestampText.text = dateFormatter.format(Date(sms.timestamp))
            if (sms.synced) {
                syncStatusText.text = "Synced"
                syncStatusText.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                syncStatusText.text = "Pending"
                syncStatusText.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SmsEntity>() {
        override fun areItemsTheSame(oldItem: SmsEntity, newItem: SmsEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SmsEntity, newItem: SmsEntity) = oldItem == newItem
    }
}
