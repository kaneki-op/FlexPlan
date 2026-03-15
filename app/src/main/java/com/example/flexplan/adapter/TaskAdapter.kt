package com.example.flexplan.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.flexplan.R
import com.example.flexplan.model.Task
import com.google.android.material.card.MaterialCardView

class TaskAdapter(
    private var tasks: List<Task>,
    private val useSummaryLayout: Boolean = false,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.taskCard)
        val tvTime: TextView = view.findViewById(R.id.tvTaskTime)
        val tvDuration: TextView? = view.findViewById(R.id.tvTaskDuration)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvDesc: TextView? = view.findViewById(R.id.tvTaskDesc)
        val tvAdjusted: TextView? = view.findViewById(R.id.tvAdjusted)
        val ivStatus: ImageView = view.findViewById(R.id.ivStatusIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val layout = if (useSummaryLayout) R.layout.item_task_summary else R.layout.item_task
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvTitle.text = task.title
        holder.tvTime.text = task.time
        
        // Handle optional fields
        holder.tvDuration?.text = " • ${task.durationMinutes}m"
        holder.tvDesc?.text = task.description
        
        // Show "FlexPlan Adjusted" label if the task was auto-adjusted
        if (task.autoAdjusted == 1) {
            holder.tvAdjusted?.visibility = View.VISIBLE
        } else {
            holder.tvAdjusted?.visibility = View.GONE
        }

        val context = holder.itemView.context
        val boxColor = ContextCompat.getColor(context, R.color.theme_surface)
        val accentColor = ContextCompat.getColor(context, R.color.theme_accent)
        val wisteriaColor = ContextCompat.getColor(context, R.color.wisteria)
        val whiteColor = ContextCompat.getColor(context, R.color.white)

        if (task.status == "completed") {
            holder.ivStatus.setImageResource(R.drawable.ic_check_done)
            holder.ivStatus.setColorFilter(accentColor)
            holder.ivStatus.alpha = 1.0f
            
            holder.card.setCardBackgroundColor(boxColor)
            holder.card.strokeColor = wisteriaColor
            
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.5f
            holder.tvTime.alpha = 0.5f
            holder.tvDuration?.alpha = 0.5f
            holder.tvDesc?.alpha = 0.5f
            holder.card.alpha = 0.8f
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_checkbox_unselected)
            holder.ivStatus.setColorFilter(wisteriaColor)
            holder.ivStatus.alpha = 0.6f
            
            holder.card.setCardBackgroundColor(boxColor)
            holder.card.strokeColor = Color.parseColor("#1AFFFFFF")
            
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1.0f
            holder.tvTime.alpha = 1.0f
            holder.tvDuration?.alpha = 1.0f
            holder.tvDesc?.alpha = 1.0f
            holder.card.alpha = 1.0f
        }

        holder.itemView.setOnClickListener { onTaskClick(task) }
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(task)
            true
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
