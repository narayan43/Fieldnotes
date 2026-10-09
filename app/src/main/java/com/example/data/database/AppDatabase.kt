package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.GoalDao
import com.example.data.dao.GoalSessionDao
import com.example.data.dao.NoteDao
import com.example.data.dao.RunningTimerDao
import com.example.data.dao.SectionDao
import com.example.data.dao.TagDao
import com.example.data.dao.WritingSessionDao
import com.example.data.entity.Goal
import com.example.data.entity.GoalSession
import com.example.data.entity.Note
import com.example.data.entity.NoteImage
import com.example.data.entity.RunningTimer
import com.example.data.entity.Section
import com.example.data.entity.SubGoal
import com.example.data.entity.Tag
import com.example.data.entity.WritingSession

@Database(
    entities = [
        Section::class,
        Tag::class,
        Note::class,
        NoteImage::class,
        WritingSession::class,
        Goal::class,
        SubGoal::class,
        GoalSession::class,
        RunningTimer::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sectionDao(): SectionDao
    abstract fun tagDao(): TagDao
    abstract fun noteDao(): NoteDao
    abstract fun writingSessionDao(): WritingSessionDao
    abstract fun goalDao(): GoalDao
    abstract fun goalSessionDao(): GoalSessionDao
    abstract fun runningTimerDao(): RunningTimerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fieldnotes.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
