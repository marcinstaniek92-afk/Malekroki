package com.malekroki.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

/** Local dates are stored as epoch days: time-zone free and correctly ordered. */
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
}

@Database(
    entities = [
        HabitEntity::class,
        CompletionEntity::class,
        SessionEntity::class,
        ReflectionEntity::class,
        LeaveEntity::class,
        LeaveHabitEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao
    abstract fun sessionDao(): SessionDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun leaveDao(): LeaveDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "male-kroki.db").build()
    }
}
