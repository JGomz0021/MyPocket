package com.mypocket.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MovementType { INCOME, EXPENSE }

data class Movement(
    val id: Long,
    val amount: Double,
    val type: MovementType,
    val category: String,
    val createdAt: Long
)

data class AppSettings(val darkTheme: Boolean = false, val fontScale: Float = 1f)

class PocketRepository(context: Context) : SQLiteOpenHelper(context, "mypocket.db", null, 1) {
    var movements by mutableStateOf(emptyList<Movement>())
        private set
    var settings by mutableStateOf(AppSettings())
        private set

    init {
        reload()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE movements (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL, type TEXT, category TEXT, created_at INTEGER)")
        db.execSQL("CREATE TABLE prefs (key TEXT PRIMARY KEY, value TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addMovement(amount: Double, type: MovementType, category: String) {
        writableDatabase.insert("movements", null, ContentValues().apply {
            put("amount", amount)
            put("type", type.name)
            put("category", category)
            put("created_at", System.currentTimeMillis())
        })
        reload()
    }

    fun deleteMovement(id: Long) {
        writableDatabase.delete("movements", "id = ?", arrayOf(id.toString()))
        reload()
    }

    fun updateSettings(darkTheme: Boolean = settings.darkTheme, fontScale: Float = settings.fontScale) {
        val db = writableDatabase
        db.insertWithOnConflict("prefs", null, ContentValues().apply { put("key", "dark_theme"); put("value", darkTheme.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
        db.insertWithOnConflict("prefs", null, ContentValues().apply { put("key", "font_scale"); put("value", fontScale.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
        settings = AppSettings(darkTheme, fontScale)
    }

    private fun reload() {
        val list = mutableListOf<Movement>()
        readableDatabase.rawQuery("SELECT * FROM movements ORDER BY created_at DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(Movement(
                    cursor.getLong(0), cursor.getDouble(1),
                    MovementType.valueOf(cursor.getString(2)), cursor.getString(3), cursor.getLong(4)
                ))
            }
        }
        movements = list

        var dark = false
        var scale = 1f
        readableDatabase.rawQuery("SELECT * FROM prefs", null).use { cursor ->
            while (cursor.moveToNext()) {
                when (cursor.getString(0)) {
                    "dark_theme" -> dark = cursor.getString(1).toBoolean()
                    "font_scale" -> scale = cursor.getString(1).toFloatOrNull() ?: 1f
                }
            }
        }
        settings = AppSettings(dark, scale)
    }
}
