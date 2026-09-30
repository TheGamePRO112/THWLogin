package com.example.thwlogin

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WorkEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // format dd.MM.yyyy (or YYYY-MM-DD for legacy)
    val startTime: String, // format HH:mm
    val endTime: String, // format HH:mm
    val durationMinutes: Long,
    val note: String = "Dienst"
)

object WorkTimeManager {
    private const val PREFS_NAME = "MeineBarcodeAppPrefs"
    private const val KEY_IS_TRACKING = "work_is_tracking"
    private const val KEY_START_TIME = "work_start_time"
    private const val KEY_ENTRIES = "work_entries_json"

    fun isTracking(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_TRACKING, false)
    }

    fun getStartTimeMillis(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_START_TIME, 0L)
    }

    fun startTracking(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_TRACKING, true)
            .putLong(KEY_START_TIME, System.currentTimeMillis())
            .apply()
    }

    fun stopTracking(context: Context, note: String = "Dienst"): WorkEntry? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!isTracking(context)) return null

        val startTimeMillis = getStartTimeMillis(context)
        val endTimeMillis = System.currentTimeMillis()
        val durationMinutes = maxOf(1L, (endTimeMillis - startTimeMillis) / (1000 * 60))

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.GERMANY)

        val dateStr = dateFormat.format(Date(startTimeMillis))
        val startTimeStr = timeFormat.format(Date(startTimeMillis))
        val endTimeStr = timeFormat.format(Date(endTimeMillis))

        val entry = WorkEntry(
            date = dateStr,
            startTime = startTimeStr,
            endTime = endTimeStr,
            durationMinutes = durationMinutes,
            note = note
        )

        prefs.edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .putLong(KEY_START_TIME, 0L)
            .apply()

        addEntry(context, entry)
        return entry
    }

    fun getEntries(context: Context): List<WorkEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ENTRIES, "[]") ?: "[]"
        val list = mutableListOf<WorkEntry>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawDate = obj.optString("date", "")
                list.add(
                    WorkEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        date = formatDateToGerman(rawDate),
                        startTime = obj.optString("startTime", ""),
                        endTime = obj.optString("endTime", ""),
                        durationMinutes = obj.optLong("durationMinutes", 0L),
                        note = obj.optString("note", "Dienst")
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { getSortKey(it.date) + " " + it.startTime }
    }

    fun saveEntries(context: Context, entries: List<WorkEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("date", entry.date)
                put("startTime", entry.startTime)
                put("endTime", entry.endTime)
                put("durationMinutes", entry.durationMinutes)
                put("note", entry.note)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    fun addEntry(context: Context, entry: WorkEntry) {
        val current = getEntries(context).toMutableList()
        current.add(0, entry)
        saveEntries(context, current)
    }

    fun updateEntry(context: Context, updated: WorkEntry) {
        val current = getEntries(context).map {
            if (it.id == updated.id) updated else it
        }
        saveEntries(context, current)
    }

    fun deleteEntry(context: Context, id: String) {
        val current = getEntries(context).filter { it.id != id }
        saveEntries(context, current)
    }

    fun formatMinutesToHours(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatDateToGerman(dateStr: String): String {
        if (dateStr.contains("-")) {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                return "${parts[2]}.${parts[1]}.${parts[0]}"
            }
        }
        return dateStr
    }

    fun getSortKey(dateStr: String): String {
        if (dateStr.contains(".")) {
            val parts = dateStr.split(".")
            if (parts.size == 3) {
                return "${parts[2]}${parts[1]}${parts[0]}"
            }
        } else if (dateStr.contains("-")) {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                return "${parts[0]}${parts[1]}${parts[2]}"
            }
        }
        return dateStr
    }

    fun getTodayGermanDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
        return dateFormat.format(Date())
    }
}
