package com.vfs.taskmanagervfsjds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        val currentUser = Cloud.auth.currentUser ?: return

        // Find the views
        val titleTextView = holder.rootView.findViewById<TextView>(R.id.TaskTitleText_Id)
        val descriptionTextView = holder.rootView.findViewById<TextView>(R.id.descriptionText_id)
        val editButton = holder.rootView.findViewById<Button>(R.id.EditTask_id)
        val deleteButton = holder.rootView.findViewById<Button>(R.id.deletebtn_id)
        val subTaskContainer = holder.rootView.findViewById<LinearLayout>(R.id.subTaskContainer_id)
        val inviteButton = holder.rootView.findViewById<Button>(R.id.SendInvite_id)
        val resetButton = holder.rootView.findViewById<Button>(R.id.ResetAndDisconnect_id)

        // Set initial data
        titleTextView.text = task.groupTitle
        descriptionTextView.text = task.description
        
        // Show reset button only for host
        if (task.hostUid == currentUser.uid) {
            resetButton.visibility = View.VISIBLE
        } else {
            resetButton.visibility = View.GONE
        }

        // Clear container for recycled view
        subTaskContainer.removeAllViews()

        // Dynamically add subtasks
        task.subTasks.forEachIndexed { index, subTask ->
            val checkBox = CheckBox(holder.rootView.context).apply {
                text = subTask.title
                isChecked = subTask.isCompleted
                setOnCheckedChangeListener { _, isChecked ->
                    subTask.isCompleted = isChecked
                    updateSubTaskInCloud(task, index, isChecked)
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
            deleteGroupFromCloud(task)
            listener.onDelete(position) 
        }

        inviteButton.setOnClickListener {
            showInviteDialog(holder.rootView.context, task)
        }

        resetButton.setOnClickListener {
            resetGroupAndDisconnect(holder.rootView.context, task)
        }
    }

    private fun resetGroupAndDisconnect(context: android.content.Context, task: Task) {
        val currentUser = Cloud.auth.currentUser ?: return
        if (task.hostUid != currentUser.uid) return

        // Clear subtasks
        task.subTasks.forEach { it.isCompleted = false }
        
        // Remove from all allowed users' Groups
        task.allowedUsers.forEach { uid ->
            db.reference.child("Groups").child(uid).child(task.groupTitle).removeValue()
        }
        
        // Clear allowed users list
        task.allowedUsers.clear()
        
        // Sync the host's version (now reset and solo)
        syncGroupToCloud(task)
        notifyDataSetChanged()
        Toast.makeText(context, "List reset and users disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun showInviteDialog(context: android.content.Context, task: Task) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Invite User to ${task.groupTitle}")
        
        val input = EditText(context)
        input.hint = "User Email"
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                if (email == Cloud.auth.currentUser?.email) {
                    Toast.makeText(context, "You cannot invite yourself", Toast.LENGTH_SHORT).show()
                } else {
                    searchAndSendInvite(context, email, task)
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun searchAndSendInvite(context: android.content.Context, email: String, task: Task) {
        db.reference.child("users").orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val receiverUid = userSnapshot.key
                            if (receiverUid != null) {
                                sendInvite(context, receiverUid, task)
                                return
                            }
                        }
                    } else {
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendInvite(context: android.content.Context, receiverUid: String, task: Task) {
        val senderUid = Cloud.auth.currentUser?.uid ?: return
        val senderEmail = Cloud.auth.currentUser?.email ?: ""
        
        val inviteMap = mapOf(
            "senderUid" to senderUid,
            "senderEmail" to senderEmail,
            "groupTitle" to task.groupTitle,
            "task" to task
        )

        db.reference.child("invites").child(receiverUid).push().setValue(inviteMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Invite sent!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun syncGroupToCloud(task: Task) {
        Cloud.auth.currentUser?.let { user ->
            // Host info check
            if (task.hostUid.isEmpty()) {
                task.hostUid = user.uid
            }

            // Sync to host's groups
            db.reference
                .child("Groups")
                .child(user.uid)
                .child(task.groupTitle)
                .setValue(task)

            // Sync to all allowed users' groups
            task.allowedUsers.forEach { allowedUid ->
                db.reference
                    .child("Groups")
                    .child(allowedUid)
                    .child(task.groupTitle)
                    .setValue(task)
            }
        }
    }

    private fun updateSubTaskInCloud(task: Task, subTaskIndex: Int, status: Boolean) {
        val hostUid = if (task.hostUid.isNotEmpty()) task.hostUid else Cloud.auth.currentUser?.uid ?: return
        
        // Update host
        db.reference.child("Groups").child(hostUid).child(task.groupTitle)
            .child("subTasks").child(subTaskIndex.toString()).child("completed").setValue(status)

        // Update all allowed users
        task.allowedUsers.forEach { uid ->
            db.reference.child("Groups").child(uid).child(task.groupTitle)
                .child("subTasks").child(subTaskIndex.toString()).child("completed").setValue(status)
        }
    }

    private fun deleteGroupFromCloud(task: Task) {
        Cloud.auth.currentUser?.let { user ->
            // If host deletes, remove for everyone? Or just for self? 
            // Standard behavior: remove for self. If host, maybe warning.
            db.reference.child("Groups").child(user.uid).child(task.groupTitle).removeValue()
            
            // Remove current user from allowed list if they are not host
            if (task.hostUid != user.uid) {
                // This would require updating the host's copy to remove this user from allowedUsers
                db.reference.child("Groups").child(task.hostUid).child(task.groupTitle)
                    .child("allowedUsers").addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            val list = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                            if (list.remove(user.uid)) {
                                db.reference.child("Groups").child(task.hostUid).child(task.groupTitle)
                                    .child("allowedUsers").setValue(list)
                            }
                        }
                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                    })
            }
        }
    }
}
