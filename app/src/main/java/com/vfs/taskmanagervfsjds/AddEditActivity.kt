package com.vfs.taskmanagervfsjds

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AddEditActivity : AppCompatActivity() {

    lateinit var saveButton: Button
    lateinit var taskTitleInput: EditText
    lateinit var taskDescriptionInput: EditText
    lateinit var headerTitle: TextView
    
    lateinit var subTaskInput: EditText
    lateinit var btnAddSubTask: Button
    lateinit var subTaskContainer: LinearLayout

    private var currentSubTasks = mutableListOf<SubTask>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit)

        saveButton = findViewById(R.id.saveTask_id)
        taskTitleInput = findViewById(R.id.editTaskTitle_id)
        taskDescriptionInput = findViewById(R.id.EditTextDescription_id)
        headerTitle = findViewById(R.id.TitleFunction_id)
        
        subTaskInput = findViewById(R.id.editSubTask_id)
        btnAddSubTask = findViewById(R.id.btnAddSubTask_id)
        subTaskContainer = findViewById(R.id.subTaskListContainer_id)

        val isAdding = intent.getBooleanExtra("Add", true)

        if (!isAdding) {
            headerTitle.text = "Edit Group"
            val task = intent.getSerializableExtra("Task") as? Task
            task?.let {
                taskTitleInput.setText(it.groupTitle)
                taskDescriptionInput.setText(it.description)
                currentSubTasks = it.subTasks.toMutableList()
                refreshSubTaskList()
            }
        } else {
            headerTitle.text = "Add Group"
        }

        btnAddSubTask.setOnClickListener {
            val subTaskTitle = subTaskInput.text.toString().trim()
            if (subTaskTitle.isNotEmpty()) {
                currentSubTasks.add(SubTask(subTaskTitle, false))
                subTaskInput.text.clear()
                refreshSubTaskList()
            } else {
                Toast.makeText(this, "Subtask name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            val title = taskTitleInput.text.toString().trim()
            val description = taskDescriptionInput.text.toString()

            if (title.isEmpty()) {
                taskTitleInput.error = "Group name cannot be empty"
                return@setOnClickListener
            }

            val resultIntent = Intent()
            val resultTask = Task(title, description, currentSubTasks)
            resultIntent.putExtra("Task", resultTask)
            
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun refreshSubTaskList() {
        subTaskContainer.removeAllViews()
        currentSubTasks.forEachIndexed { index, subTask ->
            val subTaskView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
                setPadding(12, 8, 12, 8)
                setBackgroundColor(Color.parseColor("#A7F1F5"))
                gravity = Gravity.CENTER_VERTICAL
            }

            val tv = TextView(this).apply {
                text = subTask.title
                setTextColor(Color.parseColor("#48444E"))
                typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnDelete = Button(this).apply {
                text = "X"
                setBackgroundColor(Color.parseColor("#256F75"))
                setTextColor(Color.WHITE)
                typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
                
                // Fixed size for the delete button
                val size = (36 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
                setPadding(0, 0, 0, 0)
                
                setOnClickListener {
                    currentSubTasks.removeAt(index)
                    refreshSubTaskList()
                }
            }

            subTaskView.addView(tv)
            subTaskView.addView(btnDelete)
            subTaskContainer.addView(subTaskView)
        }
    }
}
