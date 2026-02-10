package com.vfs.taskmanagervfsjds

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
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
        val checkBox = holder.rootView.findViewById<CheckBox>(R.id.Completed_id)

        // Set initial data
        titleTextView.text = task.title
        descriptionTextView.text = task.description
        
        // Initial text based on default state (unchecked)
        checkBox.text = if (checkBox.isChecked) "Done" else "Pending"

        // Update text when state changes
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            checkBox.text = if (isChecked) "Done" else "Pending"
        }

        // Trigger the interface methods
        editButton.setOnClickListener { listener.onEdit(task, position) }
        deleteButton.setOnClickListener { listener.onDelete(position) }
    }

    fun createtask(user: android.R.string) {
        for (i in 1..10) {
            db.reference
                .child("TaskLists")
                .child("Task")
                .setValue(Task("Title", "Description"))
        }
    }


    fun checkStatus()
        {
            db.reference
                .child("TaskLists")
                .child("Title")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        TODO("Not yet implemented")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })


        }
}
