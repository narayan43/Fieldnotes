package com.example.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {

    /**
     * Human-readable duration format such as "12m", "2h 40m", or "< 1m"
     */
    fun formatHumanDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0m"
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Stopwatch / active timer format: mm:ss or hh:mm:ss
     */
    fun formatTimerClock(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDate(timestampMs: Long): String {
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        return format.format(Date(timestampMs))
    }

    fun formatDateTime(timestampMs: Long): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        return format.format(Date(timestampMs))
    }

    fun formatShortDay(timestampMs: Long): String {
        val sdf = java.text.SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
