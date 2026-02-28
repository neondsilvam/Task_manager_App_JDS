package com.vfs.taskmanagervfsjds

import java.io.Serializable

data class SubTask(var title: String = "", var isCompleted: Boolean = false) : Serializable

data class Task(
    var groupTitle: String = "",
    var description: String = "",
    var subTasks: MutableList<SubTask> = mutableListOf(),
    var hostUid: String = "",
    var allowedUsers: MutableList<String> = mutableListOf()
) : Serializable

class AppData {
    companion object {
        val taskList = mutableListOf<Task>()
    }
}
