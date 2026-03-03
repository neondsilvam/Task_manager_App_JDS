package com.vfs.taskmanagervfsjds

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.vfs.taskmanagervfsjds.AppData.Companion.taskList
import com.vfs.taskmanagervfsjds.Cloud.Companion.db

fun DisplayTasks.showLoginRediterModal ()
{
    val builder = android.app.AlertDialog.Builder(this)

    builder.setTitle("Login Options")
    builder.setMessage("Would you like to login or register?")

    builder.setPositiveButton("Login") { dialog, which ->
        val intent = Intent(this, LoginRegisterActivity::class.java)
        intent.putExtra("Type", "Login")
        startActivity(intent)
    }

    builder.setNegativeButton("Register") { dialog, which ->
        val intent = Intent(this, LoginRegisterActivity::class.java)
        intent.putExtra("Type", "Register")
        startActivity(intent)
    }

    builder.setNeutralButton("Cancel") { dialog, which ->
        dialog.cancel()
    }

    val dialog = builder.create()
    dialog.show()

}

class InviteAdapter(
    private val invites: List<DataSnapshot>,
    private val onAccept: (DataSnapshot) -> Unit,
    private val onReject: (DataSnapshot) -> Unit
) : RecyclerView.Adapter<InviteAdapter.InviteViewHolder>() {

    class InviteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.inviteText_id)
        val btnAccept: Button = view.findViewById(R.id.btnAccept_id)
        val btnReject: Button = view.findViewById(R.id.btnReject_id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.invite_item, parent, false)
        return InviteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val inviteSnapshot = invites[position]
        val senderEmail = inviteSnapshot.child("senderEmail").getValue(String::class.java) ?: "Unknown"
        val groupTitle = inviteSnapshot.child("groupTitle").getValue(String::class.java) ?: "Task Group"
        
        holder.tvInfo.text = "Invite from $senderEmail for group: $groupTitle"
        
        holder.btnAccept.setOnClickListener { onAccept(inviteSnapshot) }
        holder.btnReject.setOnClickListener { onReject(inviteSnapshot) }
    }

    override fun getItemCount(): Int = invites.size
}

class DisplayTasks : AppCompatActivity(), TaskItemListener
{

    lateinit var statusText_id: TextView
    lateinit var buttonStatus: Button
    private lateinit var adapter: MyTasksAdapter
    private var editingPosition: Int = -1

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = result.data?.getSerializableExtra("Task") as? Task
            task?.let { newTask ->
                val user = Cloud.auth.currentUser
                if (user != null && newTask.hostUid.isEmpty()) {
                    newTask.hostUid = user.uid
                }

                val isDuplicate = taskList.withIndex().any { (index, t) ->
                    index != editingPosition && t.groupTitle.equals(newTask.groupTitle, ignoreCase = true)
                }

                if (isDuplicate) {
                    Toast.makeText(this, "A group with this name already exists!", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                if (editingPosition != -1) {
                    taskList[editingPosition] = newTask
                    adapter.notifyItemChanged(editingPosition)
                    editingPosition = -1
                } else {
                    taskList.add(newTask)
                    adapter.notifyItemInserted(taskList.size - 1)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Cloud.auth = FirebaseAuth.getInstance()
        statusText_id = findViewById(R.id.statusText_id)
        
        Cloud.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                statusText_id.text = "Logged in: ${user.email}"
                fetchTasksFromFirebase(user.uid)
            } else {
                statusText_id.text = "Not logged in"
                taskList.clear()
                if (::adapter.isInitialized) adapter.notifyDataSetChanged()
            }
        }

        buttonStatus = findViewById(R.id.DisplayLoginButton_id)
        buttonStatus.setOnClickListener { showLoginRediterModal() }

        val viewInvitesButton = findViewById<Button>(R.id.viewInvites_id)
        viewInvitesButton.setOnClickListener { showInvitesDialog() }

        val addButton = findViewById<Button>(R.id.saveTask_id)
        val recyclerView = findViewById<RecyclerView>(R.id.tskList_id)

        addButton.setOnClickListener {
            editingPosition = -1
            val intent = Intent(this, AddEditActivity::class.java)
            intent.putExtra("Add", true)
            startForResult.launch(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyTasksAdapter(taskList, this)
        recyclerView.adapter = adapter
    }

    private fun showInvitesDialog() {
        val user = Cloud.auth.currentUser ?: return
        val invitesRef = db.reference.child("invites").child(user.uid)

        invitesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@DisplayTasks, "No pending invites", Toast.LENGTH_SHORT).show()
                    return
                }

                val inviteList = snapshot.children.toList()
                val recyclerView = RecyclerView(this@DisplayTasks)
                recyclerView.layoutManager = LinearLayoutManager(this@DisplayTasks)
                
                val builder = AlertDialog.Builder(this@DisplayTasks)
                builder.setTitle("Your Invites")
                builder.setView(recyclerView)
                builder.setNegativeButton("Close", null)

                val dialog = builder.create()

                val inviteAdapter = InviteAdapter(inviteList, 
                    onAccept = { inviteSnapshot ->
                        acceptInvite(inviteSnapshot)
                        dialog.dismiss()
                    },
                    onReject = { inviteSnapshot ->
                        inviteSnapshot.ref.removeValue()
                        Toast.makeText(this@DisplayTasks, "Invite rejected", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                )
                recyclerView.adapter = inviteAdapter

                // Fixed height if more than 4 invites
                if (inviteList.size > 4) {
                    recyclerView.post {
                        val params = recyclerView.layoutParams
                        // Approximate height of 4 items. A more robust way would be measuring children.
                        // Assuming each item is roughly 120dp.
                        val density = resources.displayMetrics.density
                        params.height = (400 * density).toInt() 
                        recyclerView.layoutParams = params
                    }
                }

                dialog.show()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun acceptInvite(inviteSnapshot: DataSnapshot) {
        val task = inviteSnapshot.child("task").getValue(Task::class.java) ?: return
        val user = Cloud.auth.currentUser ?: return

        // 1. Update the task locally: Add self to allowedUsers
        if (!task.allowedUsers.contains(user.uid)) {
            task.allowedUsers.add(user.uid)
        }

        // 2. Add task locally if not exists
        if (!taskList.any { it.groupTitle == task.groupTitle }) {
            taskList.add(task)
            adapter.notifyItemInserted(taskList.size - 1)
        }

        // 3. Save to user's Firebase Groups
        db.reference.child("Groups").child(user.uid).child(task.groupTitle).setValue(task)
            .addOnSuccessListener {
                // 4. Update Host's copy to include this user in allowedUsers
                db.reference.child("Groups").child(task.hostUid).child(task.groupTitle)
                    .child("allowedUsers").setValue(task.allowedUsers)

                Toast.makeText(this, "Invite accepted!", Toast.LENGTH_SHORT).show()
                inviteSnapshot.ref.removeValue()
            }
    }

    private fun fetchTasksFromFirebase(uid: String) {
        val databaseRef = Cloud.db.reference.child("Groups").child(uid)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var changesMade = false
                for (taskSnapshot in snapshot.children) {
                    val groupTask = taskSnapshot.getValue(Task::class.java)
                    if (groupTask != null && groupTask.groupTitle.isNotEmpty()) {
                        val existingIndex = taskList.indexOfFirst { it.groupTitle.equals(groupTask.groupTitle, ignoreCase = true) }
                        if (existingIndex == -1) {
                            taskList.add(groupTask)
                            changesMade = true
                        } else {
                            // Update existing
                            taskList[existingIndex] = groupTask
                            changesMade = true
                        }
                    }
                }
                if (changesMade) {
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DisplayTasks, "Cloud sync failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onEdit(task: Task, position: Int) {
        editingPosition = position
        val intent = Intent(this, AddEditActivity::class.java)
        intent.putExtra("Add", false)
        intent.putExtra("Task", task) // Passing the whole object
        startForResult.launch(intent)
    }

    override fun onDelete(position: Int) {
        val task = taskList[position]
        taskList.removeAt(position)
        adapter.notifyItemRemoved(position)
        adapter.notifyItemRangeChanged(position, taskList.size)
    }
}
