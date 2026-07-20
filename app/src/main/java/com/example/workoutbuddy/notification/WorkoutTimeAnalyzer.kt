package com.example.workoutbuddy.notification

import com.example.workoutbuddy.data.database.WorkoutDao
import java.util.Calendar

data class ReminderTime(val hour: Int, val minute: Int)

class WorkoutTimeAnalyzer(private val dao: WorkoutDao) {

    fun computeReminderTime(): ReminderTime {
        val workouts = dao.getLast100CompletedWorkouts()
        if (workouts.isEmpty()) return DEFAULT_TIME

        val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        // Group start times (in minutes since midnight) by day-of-week
        val byDow = mutableMapOf<Int, MutableList<Int>>()
        for (workout in workouts) {
            val cal = Calendar.getInstance().apply { timeInMillis = workout.date }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            byDow.getOrPut(dow) { mutableListOf() }.add(minuteOfDay)
        }

        val minuteOfDay = when {
            (byDow[todayDow]?.size ?: 0) >= MIN_SAMPLES_FOR_DOW ->
                byDow[todayDow]!!.average().toInt()
            byDow.values.flatten().isNotEmpty() ->
                byDow.values.flatten().average().toInt()
            else -> return DEFAULT_TIME
        }

        val reminded = minuteOfDay - LEAD_MINUTES_BEFORE
        val clampedHour = (reminded / 60).coerceIn(0, 23)
        val clampedMinute = (reminded % 60).let { if (it < 0) 0 else it }
        return ReminderTime(clampedHour, clampedMinute)
    }

    companion object {
        val DEFAULT_TIME = ReminderTime(8, 0)
        private const val LEAD_MINUTES_BEFORE = 5
        private const val MIN_SAMPLES_FOR_DOW = 2
    }
}
