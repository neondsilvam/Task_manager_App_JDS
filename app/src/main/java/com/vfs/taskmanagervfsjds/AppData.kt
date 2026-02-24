package com.vfs.taskmanagervfsjds

import java.io.Serializable

data class SubTask(var title: String = "", var isCompleted: Boolean = false) : Serializable

data class Task(
    var groupTitle: String = "",
    var description: String = "",
    var subTasks: MutableList<SubTask> = mutableListOf()
) : Serializable

class AppData {
    companion object {
        val taskList = mutableListOf(
            Task("Shopping", "Buy groceries", mutableListOf(
                SubTask("Milk", false),
                SubTask("Eggs", false)
            )),
            Task("Work", "Office tasks", mutableListOf(
                SubTask("Finish report", false),
                SubTask("Email client", false)
            )),
            Task("Personal", "Home errands", mutableListOf(
                SubTask("Call mom", false)
            ))
        )
    }
}
