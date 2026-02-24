package com.vfs.taskmanagervfsjds

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vfs.taskmanagervfsjds.Cloud.Companion.db

//viewHolder for the recycler
class MyViewHolder(val rootView: LinearLayout) : RecyclerView.ViewHolder(rootView)

//the interface call from the DisplayTask
interface TaskItemListener {
    fun onEdit(task: Task, position: Int)
    fun onDelete(position: Int)
}

//The adapter for the recycler
class MyTasksAdapter(
    private val inputData: List<Task>,
    private val listener: TaskItemListener
) : RecyclerView.Adapter<MyViewHolder>() {
    override fun getItemCount(): Int = inputData.size

    //Creates a new viewHolder base on the LinearLayout taskpost display
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val linearLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.taskpost, parent, false) as LinearLayout
        return MyViewHolder(linearLayout)
    }

    //Binds the data to the viewHolder recycler
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val task = inputData[position]

        // Find the views
        val titleTextView = holder.rootView.findViewById<TextView>(R.id.TaskTitleText_Id)
        val descriptionTextView = holder.rootView.findViewById<TextView>(R.id.descriptionText_id)
        val editButton = holder.rootView.findViewById<Button>(R.id.EditTask_id)
        val deleteButton = holder.rootView.findViewById<Button>(R.id.deletebtn_id)
        val subTaskContainer = holder.rootView.findViewById<LinearLayout>(R.id.subTaskContainer_id)

        // Set initial data
        titleTextView.text = task.groupTitle
        descriptionTextView.text = task.description
        
        // Clear container for recycled view
        subTaskContainer.removeAllViews()

        // Dynamically add subtasks
        task.subTasks.forEachIndexed { index, subTask ->
            val checkBox = CheckBox(holder.rootView.context).apply {
                text = subTask.title
                isChecked = subTask.isCompleted
                setOnCheckedChangeListener { _, isChecked ->
                    subTask.isCompleted = isChecked
                    updateSubTaskInCloud(task.groupTitle, index, isChecked)
                }
            }
            subTaskContainer.addView(checkBox)
        }

        syncGroupToCloud(task)

        // Trigger the interface methods
        editButton.setOnClickListener {
            listener.onEdit(task, position) 
        }
        deleteButton.setOnClickListener {
            deleteGroupFromCloud(task.groupTitle)
            listener.onDelete(position) 
        }
    }

    private fun syncGroupToCloud(task: Task) {
        Cloud.auth.currentUser?.let { user ->
            db.reference
                .child("Groups") // Moved from users table to Groups table
                .child(user.uid)
                .child(task.groupTitle)
                .setValue(task)
        }
    }

    private fun updateSubTaskInCloud(groupTitle: String, subTaskIndex: Int, status: Boolean) {
        Cloud.auth.currentUser?.let { user ->
            db.reference
                .child("Groups") // Moved from users table to Groups table
                .child(user.uid)
                .child(groupTitle)
                .child("subTasks")
                .child(subTaskIndex.toString())
                .child("completed")
                .setValue(status)
        }
    }

    private fun deleteGroupFromCloud(groupTitle: String) {
        Cloud.auth.currentUser?.let { user ->
            db.reference
                .child("Groups") // Moved from users table to Groups table
                .child(user.uid)
                .child(groupTitle)
                .removeValue()
        }
    }
}
