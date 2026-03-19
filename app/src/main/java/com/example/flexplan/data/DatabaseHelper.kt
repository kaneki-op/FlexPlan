package com.example.flexplan.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.flexplan.model.Task
import com.example.flexplan.model.User
import org.mindrot.jbcrypt.BCrypt
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "FlexPlan.db"
        private const val DATABASE_VERSION = 4

        // Users Table
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "user_id"
        const val COL_NAME = "name"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
        const val COL_AGE = "age"
        const val COL_CREATED_AT = "created_at"

        // Tasks Table
        const val TABLE_TASKS = "tasks"
        const val COL_TASK_ID = "task_id"
        const val COL_TASK_USER_ID = "user_id"
        const val COL_TASK_TITLE = "task_title"
        const val COL_TASK_DESC = "task_description"
        const val COL_TASK_TIME = "task_time"
        const val COL_DURATION = "duration_minutes"
        const val COL_TASK_STATUS = "task_status"
        const val COL_COMPLETION_TIME = "completion_time"
        const val COL_TASK_DATE = "task_date"
        const val COL_DELAY_MINUTES = "delay_minutes"
        const val COL_AUTO_ADJUSTED = "auto_adjusted"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT, "
                + COL_EMAIL + " TEXT UNIQUE, "
                + COL_PASSWORD + " TEXT, "
                + COL_AGE + " INTEGER, "
                + COL_CREATED_AT + " DEFAULT CURRENT_TIMESTAMP" + ")")

        val createTasksTable = ("CREATE TABLE " + TABLE_TASKS + " ("
                + COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TASK_USER_ID + " INTEGER, "
                + COL_TASK_TITLE + " TEXT, "
                + COL_TASK_DESC + " TEXT, "
                + COL_TASK_TIME + " TEXT, "
                + COL_DURATION + " INTEGER DEFAULT 30, "
                + COL_TASK_STATUS + " TEXT DEFAULT 'pending', "
                + COL_COMPLETION_TIME + " TEXT, "
                + COL_TASK_DATE + " TEXT, "
                + COL_DELAY_MINUTES + " INTEGER DEFAULT 0, "
                + COL_AUTO_ADJUSTED + " INTEGER DEFAULT 0, "
                + "FOREIGN KEY(" + COL_TASK_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))")

        db?.execSQL(createUsersTable)
        db?.execSQL(createTasksTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_COMPLETION_TIME TEXT")
            db?.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_TASK_DATE TEXT")
            db?.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_DELAY_MINUTES INTEGER DEFAULT 0")
        }
        if (oldVersion < 3) {
            db?.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_AUTO_ADJUSTED INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) {
            db?.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $COL_DURATION INTEGER DEFAULT 30")
        }
    }

    // --- User Operations ---

    fun registerUser(user: User): Long {
        val db = this.writableDatabase
        // Hash the password before saving
        val hashedPassword = BCrypt.hashpw(user.password, BCrypt.gensalt())
        
        val contentValues = ContentValues().apply {
            put(COL_NAME, user.name)
            put(COL_EMAIL, user.email)
            put(COL_PASSWORD, hashedPassword)
            put(COL_AGE, user.age)
        }
        val result = db.insert(TABLE_USERS, null, contentValues)
        db.close()
        return result
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_PASSWORD FROM $TABLE_USERS WHERE $COL_EMAIL=?",
            arrayOf(email)
        )
        
        var isValid = false
        if (cursor.moveToFirst()) {
            val storedHash = cursor.getString(0)
            try {
                isValid = BCrypt.checkpw(password, storedHash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        cursor.close()
        db.close()
        return isValid
    }

    fun getUserByEmail(email: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL=?", arrayOf(email))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
            )
        }
        cursor.close()
        db.close()
        return user
    }

    fun updateUser(email: String, name: String, age: Int): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_AGE, age)
        }
        val result = db.update(TABLE_USERS, contentValues, "$COL_EMAIL=?", arrayOf(email))
        db.close()
        return result
    }

    // --- Task Operations ---

    fun addTask(task: Task): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_TASK_USER_ID, task.userId)
            put(COL_TASK_TITLE, task.title)
            put(COL_TASK_DESC, task.description)
            put(COL_TASK_TIME, task.time)
            put(COL_DURATION, task.durationMinutes)
            put(COL_TASK_STATUS, task.status)
            put(COL_TASK_DATE, task.taskDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            put(COL_AUTO_ADJUSTED, task.autoAdjusted)
        }
        val result = db.insert(TABLE_TASKS, null, contentValues)
        db.close()
        return result
    }

    fun isTimeTaken(userId: Int, checkTimeStr: String): Boolean {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val checkDate = try { sdf.parse(checkTimeStr) } catch (e: Exception) { null } ?: return false
        
        val tasks = getTasksByUserId(userId)
        for (task in tasks) {
            if (task.status == "pending") {
                val startTime = try { sdf.parse(task.time) } catch (e: Exception) { null } ?: continue
                val endTimeCalendar = Calendar.getInstance().apply {
                    time = startTime
                    add(Calendar.MINUTE, task.durationMinutes)
                }
                val endTime = endTimeCalendar.time
                
                // If checkTime falls between task's start and end time
                if (checkDate.equals(startTime) || (checkDate.after(startTime) && checkDate.before(endTime))) {
                    return true
                }
            }
        }
        return false
    }

    fun getNextFreeSlot(userId: Int, startTime: String): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            val date = sdf.parse(startTime)
            if (date != null) {
                calendar.time = date
                for (i in 1..48) {
                    calendar.add(Calendar.MINUTE, 30)
                    val nextTime = sdf.format(calendar.time)
                    if (!isTimeTaken(userId, nextTime)) {
                        return nextTime
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return startTime
    }

    fun getTasksByUserId(userId: Int): List<Task> {
        val taskList = mutableListOf<Task>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TASKS WHERE $COL_TASK_USER_ID=?", arrayOf(userId.toString()))
        
        if (cursor.moveToFirst()) {
            do {
                val task = Task(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TASK_USER_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DESC)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_TIME)),
                    durationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DURATION)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_STATUS)),
                    completionTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_COMPLETION_TIME)),
                    taskDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_TASK_DATE)),
                    delayMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DELAY_MINUTES)),
                    autoAdjusted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AUTO_ADJUSTED))
                )
                taskList.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return taskList.sortedBy { task ->
            try {
                sdf.parse(task.time)
            } catch (e: Exception) {
                Date(0)
            }
        }
    }

    fun updateTaskCompletion(taskId: Int, status: String, completionTime: String?, delay: Int): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_TASK_STATUS, status)
            put(COL_COMPLETION_TIME, completionTime)
            put(COL_DELAY_MINUTES, delay)
        }
        val result = db.update(TABLE_TASKS, contentValues, "$COL_TASK_ID=?", arrayOf(taskId.toString()))
        db.close()
        return result
    }

    fun updateTaskTime(taskId: Int, newTime: String, autoAdjusted: Int): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_TASK_TIME, newTime)
            put(COL_AUTO_ADJUSTED, autoAdjusted)
        }
        val result = db.update(TABLE_TASKS, contentValues, "$COL_TASK_ID=?", arrayOf(taskId.toString()))
        db.close()
        return result
    }

    fun shiftFutureTasks(userId: Int, fromTime: String, shiftMinutes: Int) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val tasks = getTasksByUserId(userId)
        val startTime = try { sdf.parse(fromTime) } catch (e: Exception) { null } ?: return

        for (task in tasks) {
            if (task.status == "pending") {
                val taskTime = try { sdf.parse(task.time) } catch (e: Exception) { null } ?: continue
                if (taskTime.after(startTime)) {
                    val calendar = Calendar.getInstance()
                    calendar.time = taskTime
                    calendar.add(Calendar.MINUTE, shiftMinutes)
                    val newTime = sdf.format(calendar.time)
                    updateTaskTime(task.id!!, newTime, 1)
                }
            }
        }
    }

    fun deleteTask(taskId: Int): Int {
        val db = this.writableDatabase
        val result = db.delete(TABLE_TASKS, "$COL_TASK_ID=?", arrayOf(taskId.toString()))
        db.close()
        return result
    }
}
