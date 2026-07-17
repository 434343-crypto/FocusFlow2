package com.amir.focusflow

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amir.focusflow.databinding.ItemSessionBinding

class SessionAdapter(
    private val sessions: MutableList<Session>,
    private val onToggle: (Session, Boolean) -> Unit,
    private val onRename: (Session, String) -> Unit
) : RecyclerView.Adapter<SessionAdapter.VH>() {

    inner class VH(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root)

    var activeIndex: Int = -1
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = sessions.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = sessions[position]
        val b = holder.binding

        b.timeRange.text = "${minutesToTime(s.start)} — ${minutesToTime(s.end)}"
        b.sessionName.text = s.name
        b.durationLabel.text = "${s.end - s.start} min"
        b.typeLabel.text = s.type
        b.enabledCheckbox.setOnCheckedChangeListener(null)
        b.enabledCheckbox.isChecked = s.enabled

        val accent = when (s.type) {
            "study" -> Color.parseColor("#3B82F6")
            "break" -> Color.parseColor("#10B981")
            else -> Color.parseColor("#F59E0B")
        }
        b.typeLabel.setTextColor(accent)
        b.root.strokeColor = if (position == activeIndex) Color.parseColor("#FACC15") else accent
        b.root.alpha = if (s.enabled) 1.0f else 0.55f

        b.enabledCheckbox.setOnCheckedChangeListener { _, checked ->
            s.enabled = checked
            onToggle(s, checked)
            notifyItemChanged(position)
        }

        b.sessionName.isEnabled = s.type == "study"
        if (s.type == "study") {
            b.sessionName.setOnClickListener {
                onRename(s, s.name)
            }
        } else {
            b.sessionName.setOnClickListener(null)
        }
    }
}
