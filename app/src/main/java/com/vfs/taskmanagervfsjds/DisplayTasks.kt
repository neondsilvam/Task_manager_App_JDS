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

fun DisplayTasks.checkStatus() {
    if (Cloud.auth.currentUser == null) {
        statusText_id.text = "Not logged in"
    }else {
        statusText_id.text = "Logged in"
    }
}

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


// Data class to hold task information
data class Task(var title: String, var description: String)

class DisplayTasks : AppCompatActivity(), TaskItemListener
{

    //For the recycler view
    lateinit var statusText_id: TextView
    lateinit var buttonStatus: Button

    private lateinit var adapter: MyTasksAdapter
    //This is the list that holds the elements of the recycler (some are added here just as an example)
    private val taskList = mutableListOf(
        Task("Shopping", "Buy milk and eggs"),
        Task("Work", "Finish the report for tomorrow's meeting"),
        Task("Personal", "Call mom"),
        Task("Personal", "Call mom"),
        Task("Personal", "Call mom"),
        Task("Personal", "Call mom"),
        Task("Personal", "Call mom"),
        Task("Personal", "Call mom")
    )

    //This is in first referene to get the element of the list to edit
    private var editingPosition: Int = -1

    // Handle results from AddEditActivity
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val title = data?.getStringExtra("Title") ?: ""
            val desc = data?.getStringExtra("Description") ?: ""

            // Check for duplicate names (ignoring case)
            val isDuplicate = taskList.withIndex().any { (index, task) ->
                index != editingPosition && task.title.equals(title, ignoreCase = true)
            }

            if (isDuplicate) {
                Toast.makeText(this, "A task with this name already exists!", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            if (editingPosition != -1) {
                // Update existing task
                taskList[editingPosition].title = title
                taskList[editingPosition].description = desc
                adapter.notifyItemChanged(editingPosition)
                editingPosition = -1
            } else {
                // Add new task
                taskList.add(Task(title, desc))
                adapter.notifyItemInserted(taskList.size - 1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Cloud.auth = FirebaseAuth.getInstance()

        statusText_id = findViewById(R.id.statusText_id)
        checkStatus()
        buttonStatus = findViewById(R.id.DisplayLoginButton_id)
        buttonStatus.setOnClickListener { showLoginRediterModal() }



        //This one presents the button to add a new task and the integration into the recycler
        val addButton = findViewById<Button>(R.id.saveTask_id)
        val recyclerView = findViewById<RecyclerView>(R.id.tskList_id)

        //event to listen for the button
        addButton.setOnClickListener {
            editingPosition = -1
            val intent = Intent(this, AddEditActivity::class.java)
            intent.putExtra("Add", true)
            startForResult.launch(intent)
        }

        //gets the recycler view ready
        recyclerView.layoutManager = LinearLayoutManager(this)

        // adapts the content to the recycler and binds it
        adapter = MyTasksAdapter(taskList, this)
        recyclerView.adapter = adapter
    }

    // Interface implementations to handle the button to edit
    override fun onEdit(task: Task, position: Int) {
        editingPosition = position
        val intent = Intent(this, AddEditActivity::class.java)
        intent.putExtra("Add", false)
        intent.putExtra("Title", task.title)
        intent.putExtra("Description", task.description)
        startForResult.launch(intent)
    }

    //Other interface implementation to handle the button to delete
    override fun onDelete(position: Int) {
        taskList.removeAt(position)
        adapter.notifyItemRemoved(position)
        // Ensure positions stay correct in the adapter
        adapter.notifyItemRangeChanged(position, taskList.size)
    }
}
