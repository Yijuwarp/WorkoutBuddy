package com.example.workoutbuddy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        UserProfileEntity::class,
        ExercisePreferenceEntity::class,
        EquipmentPresetEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        // Adds exercise difficulty + per-exercise frequency preferences. Existing exercises get
        // difficulty backfilled from their existing impactLevel; existing user profiles get
        // difficultyCeiling defaulted to MEDIUM, since gym experience (which new profiles proxy
        // the ceiling from) isn't a reliable signal for pre-existing profiles - MEDIUM is a
        // neutral guess users can adjust from Profile settings. Written as a real migration
        // instead of relying on fallbackToDestructiveMigration() because that wipes/re-seeds,
        // which would lose existing profiles/workout history.
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN difficulty TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL(
                    "UPDATE exercises SET difficulty = CASE impactLevel " +
                        "WHEN 'HIGH' THEN 'HARD' WHEN 'LOW' THEN 'EASY' ELSE 'MEDIUM' END"
                )
                db.execSQL("ALTER TABLE user_profile ADD COLUMN difficultyCeiling TEXT")
                db.execSQL("UPDATE user_profile SET difficultyCeiling = 'MEDIUM'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exercise_preferences (" +
                        "exerciseId INTEGER NOT NULL PRIMARY KEY, " +
                        "frequency TEXT NOT NULL)"
                )
            }
        }

        // Adds equipment_presets, letting users save/re-apply named equipment setups (e.g.
        // "Home", "Gym") instead of re-toggling each piece by hand.
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS equipment_presets (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "equipmentCsv TEXT NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                )
                .addMigrations(MIGRATION_15_16, MIGRATION_16_17)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
