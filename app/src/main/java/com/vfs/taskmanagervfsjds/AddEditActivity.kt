package com.vfs.taskmanagervfsjds

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class AddEditActivity : AppCompatActivity() {

    lateinit var saveButton: Button
    lateinit var taskTitleInput: EditText
    lateinit var taskDescriptionInput: EditText
    lateinit var headerTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit)

        //Variables from the main "AddEditActivity" layout
        saveButton = findViewById<Button>(R.id.saveTask_id)
        taskTitleInput = findViewById<EditText>(R.id.editTaskTitle_id)
        taskDescriptionInput = findViewById<EditText>(R.id.EditTextDescription_id)
        headerTitle = findViewById<TextView>(R.id.TitleFunction_id)

        // Check if we are adding or editing
        val isAdding = intent.getBooleanExtra("Add", true)

        //This is a boolean system that check if the user will edit or add a task
        if (!isAdding) {
            headerTitle.text = "Edit Task"
            // Pre-fill fields if editing
            taskTitleInput.setText(intent.getStringExtra("Title"))
            taskDescriptionInput.setText(intent.getStringExtra("Description"))
        } else {
            headerTitle.text = "Add Task"
        }

        //For the return file inside the display process of the recycler view
        saveButton.setOnClickListener {
            val title = taskTitleInput.text.toString().trim()
            val description = taskDescriptionInput.text.toString()

            // Block action if Title is empty
            if (title.isEmpty()) {
                taskTitleInput.error = "Title cannot be empty"
                Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Updates the values using the Intent() system
            val resultIntent = Intent()
            resultIntent.putExtra("Title", title)
            resultIntent.putExtra("Description", description)
            
            // Return result to DisplayTasks
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
