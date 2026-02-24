package com.vfs.taskmanagervfsjds

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.vfs.taskmanagervfsjds.AppData.Companion.taskList

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
            }
        }

        buttonStatus = findViewById(R.id.DisplayLoginButton_id)
        buttonStatus.setOnClickListener { showLoginRediterModal() }

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

    private fun fetchTasksFromFirebase(uid: String) {
        val databaseRef = Cloud.db.reference.child("Groups").child(uid)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var changesMade = false
                for (taskSnapshot in snapshot.children) {
                    val groupTask = taskSnapshot.getValue(Task::class.java)
                    if (groupTask != null && groupTask.groupTitle.isNotEmpty()) {
                        val existsLocally = taskList.any { it.groupTitle.equals(groupTask.groupTitle, ignoreCase = true) }
                        if (!existsLocally) {
                            taskList.add(groupTask)
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
        taskList.removeAt(position)
        adapter.notifyItemRemoved(position)
        adapter.notifyItemRangeChanged(position, taskList.size)
    }
}
