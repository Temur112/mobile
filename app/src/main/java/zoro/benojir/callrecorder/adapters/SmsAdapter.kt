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
        private val textFromTo: TextView = itemView.findViewById(R.id.textFromTo)
        private val textBody: TextView = itemView.findViewById(R.id.textBody)
        private val textMeta: TextView = itemView.findViewById(R.id.textMeta)
        private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun bind(sms: SmsEntity) {
            val context = itemView.context

            val isOutgoing = sms.sender.equals("me", true) || sms.status.equals("sent", true)
            val directionArrow = if (isOutgoing) "➡️" else "⬅️"

            val fromTo = if (isOutgoing) {
                "$directionArrow From: ${sms.sender} → To: ${sms.receiver}"
            } else {
                "$directionArrow From: ${sms.sender} → To: me"
            }

            textFromTo.text = fromTo
            textBody.text = sms.text

            val timeStr = formatter.format(Date(sms.timestamp))
            val syncStatus = if (sms.synced) "Synced ✅" else "Pending ❌"
            textMeta.text = "Time: $timeStr | User: ${sms.username} | $syncStatus"

            // ✅ Green if synced, ❌ Red if pending
            if (sms.synced) {
                itemView.isEnabled = true
                textFromTo.setTextColor(context.getColor(android.R.color.holo_green_dark))
            } else {
                itemView.isEnabled = false
                textFromTo.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SmsEntity>() {
        override fun areItemsTheSame(oldItem: SmsEntity, newItem: SmsEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SmsEntity, newItem: SmsEntity) = oldItem == newItem
    }
}
