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
import java.text.SimpleDateFormat
import java.util.*

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
        val tvFrozen: TextView? = view.findViewById(R.id.tvFrozenLabel)
        val tvOptimized: TextView? = view.findViewById(R.id.tvOptimizedLabel)
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
        
        holder.tvDuration?.text = " • ${task.durationMinutes}m"
        holder.tvDesc?.text = task.description
        
        if (task.autoAdjusted == 1) {
            holder.tvAdjusted?.visibility = View.VISIBLE
        } else {
            holder.tvAdjusted?.visibility = View.GONE
        }

        // SMART OPTIMIZED LABEL
        if (task.isOptimized == 1) {
            holder.tvOptimized?.visibility = View.VISIBLE
        } else {
            holder.tvOptimized?.visibility = View.GONE
        }

        val context = holder.itemView.context
        
        val isFutureTask = isTaskInFuture(task)
        val isLocked = isFutureTask && task.status != "completed"

        if (isLocked) {
            holder.card.alpha = 0.5f
            holder.ivStatus.setImageResource(R.drawable.ic_lock)
            holder.ivStatus.setColorFilter(Color.GRAY)
            holder.tvFrozen?.visibility = View.VISIBLE
        } else {
            holder.card.alpha = 1.0f
            holder.tvFrozen?.visibility = View.GONE
            if (task.status == "completed") {
                holder.ivStatus.setImageResource(R.drawable.ic_check_done)
                holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.theme_accent))
            } else {
                holder.ivStatus.setImageResource(R.drawable.ic_checkbox_unselected)
                holder.ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.wisteria))
            }
        }

        if (task.status == "completed") {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.alpha = 0.5f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            if (isLocked) {
                android.widget.Toast.makeText(context, "Plan is frozen until scheduled time!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                onTaskClick(task)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            onTaskLongClick(task)
            true
        }
    }

    private fun isTaskInFuture(task: Task): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
            val taskDateTime = sdf.parse("${task.taskDate} ${task.time}")
            taskDateTime?.after(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
