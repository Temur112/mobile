package zoro.benojir.callrecorder.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import zoro.benojir.callrecorder.R
import zoro.benojir.callrecorder.data.SmsEntity

class SmsAdapter : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    private var smsList: List<SmsEntity> = emptyList()

    fun submitList(list: List<SmsEntity>) {
        smsList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sms, parent, false)
        return SmsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        val sms = smsList[position]
        holder.sender.text = "From: ${sms.sender}"
        holder.text.text = sms.text
        holder.status.text = if (sms.synced) "✅ Synced" else "❌ Not Synced"
    }

    override fun getItemCount(): Int = smsList.size

    class SmsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sender: TextView = view.findViewById(R.id.smsSender)
        val text: TextView = view.findViewById(R.id.smsText)
        val status: TextView = view.findViewById(R.id.smsStatus)
    }
}
