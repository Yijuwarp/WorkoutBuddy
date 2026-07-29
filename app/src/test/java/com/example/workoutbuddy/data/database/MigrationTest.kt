package com.example.workoutbuddy.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate15To16() {
        var db = helper.createDatabase(TEST_DB, 15).apply {
            execSQL(
                "INSERT INTO exercises (id, name, category, type, bodyPart, calorieBurnRate, description, howToSteps, impactLevel, youtubeUrl, equipment) " +
                    "VALUES (1, 'Bench Press', 'PUSH', 'LIFT', 'Chest', 5.0, 'Description', 'Steps', 'HIGH', '', '')"
            )
            execSQL(
                "INSERT INTO user_profile (id, nickname, age, height, weight, gender, strengthScore, staminaScore, gymExperience, restTimerEnabled, equipmentOwned) " +
                    "VALUES (1, 'John', 25, 175.0, 70.0, 'Male', 100.0, 100.0, 'Beginner', 1, '')"
            )
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 16, true, WorkoutDatabase.MIGRATION_15_16)

        val cursorEx = db.query("SELECT difficulty FROM exercises WHERE id = 1")
        assertTrue(cursorEx.moveToFirst())
        assertEquals("HARD", cursorEx.getString(cursorEx.getColumnIndexOrThrow("difficulty")))
        cursorEx.close()

        val cursorProf = db.query("SELECT difficultyCeiling FROM user_profile WHERE id = 1")
        assertTrue(cursorProf.moveToFirst())
        assertEquals("MEDIUM", cursorProf.getString(cursorProf.getColumnIndexOrThrow("difficultyCeiling")))
        cursorProf.close()
    }

    @Test
    fun migrate16To17() {
        helper.createDatabase(TEST_DB, 16).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 17, true, WorkoutDatabase.MIGRATION_16_17)
        db.execSQL("INSERT INTO equipment_presets (name, equipmentCsv) VALUES ('Home', 'dumbbells')")
        val cursor = db.query("SELECT * FROM equipment_presets")
        assertTrue(cursor.moveToFirst())
        cursor.close()
    }

    @Test
    fun migrate17To18() {
        helper.createDatabase(TEST_DB, 17).apply {
            execSQL(
                "INSERT INTO user_profile (id, nickname, age, height, weight, gender, strengthScore, staminaScore, gymExperience, restTimerEnabled, equipmentOwned, difficultyCeiling) " +
                    "VALUES (1, 'John', 25, 175.0, 70.0, 'Male', 100.0, 100.0, 'Beginner', 1, '', 'MEDIUM')"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 18, true, WorkoutDatabase.MIGRATION_17_18)
        val cursor = db.query("SELECT hasSeenWorkoutTour FROM user_profile WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("hasSeenWorkoutTour")))
        cursor.close()
    }

    @Test
    fun migrate18To19() {
        helper.createDatabase(TEST_DB, 18).close()
        val db = helper.runMigrationsAndValidate(TEST_DB, 19, true, WorkoutDatabase.MIGRATION_18_19)
        db.execSQL("INSERT INTO muscle_group_recovery (muscleGroup, fatiguePct, lastUpdatedAt) VALUES ('PUSH', 25.0, 1000)")
        val cursor = db.query("SELECT * FROM muscle_group_recovery WHERE muscleGroup = 'PUSH'")
        assertTrue(cursor.moveToFirst())
        cursor.close()
    }

    @Test
    fun migrate19To20() {
        helper.createDatabase(TEST_DB, 19).apply {
            execSQL(
                "INSERT INTO user_profile (id, nickname, age, height, weight, gender, strengthScore, staminaScore, gymExperience, restTimerEnabled, equipmentOwned, difficultyCeiling, hasSeenWorkoutTour) " +
                    "VALUES (1, 'John', 25, 175.0, 70.0, 'Male', 100.0, 100.0, 'Beginner', 1, '', 'MEDIUM', 0)"
            )
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 20, true, WorkoutDatabase.MIGRATION_19_20)
        val cursor = db.query("SELECT workoutLengthMinutes FROM user_profile WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals(45, cursor.getInt(cursor.getColumnIndexOrThrow("workoutLengthMinutes")))
        cursor.close()
    }

    @Test
    fun migrateAll15To20() {
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL(
                "INSERT INTO exercises (id, name, category, type, bodyPart, calorieBurnRate, description, howToSteps, impactLevel, youtubeUrl, equipment) " +
                    "VALUES (1, 'Bench Press', 'PUSH', 'LIFT', 'Chest', 5.0, 'Description', 'Steps', 'HIGH', '', '')"
            )
            execSQL(
                "INSERT INTO user_profile (id, nickname, age, height, weight, gender, strengthScore, staminaScore, gymExperience, restTimerEnabled, equipmentOwned) " +
                    "VALUES (1, 'John', 25, 175.0, 70.0, 'Male', 100.0, 100.0, 'Beginner', 1, '')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            20,
            true,
            WorkoutDatabase.MIGRATION_15_16,
            WorkoutDatabase.MIGRATION_16_17,
            WorkoutDatabase.MIGRATION_17_18,
            WorkoutDatabase.MIGRATION_18_19,
            WorkoutDatabase.MIGRATION_19_20
        )

        val cursor = db.query("SELECT * FROM exercises WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("HARD", cursor.getString(cursor.getColumnIndexOrThrow("difficulty")))
        cursor.close()

        val cursorProf = db.query("SELECT difficultyCeiling, hasSeenWorkoutTour, workoutLengthMinutes FROM user_profile WHERE id = 1")
        assertTrue(cursorProf.moveToFirst())
        assertEquals("MEDIUM", cursorProf.getString(cursorProf.getColumnIndexOrThrow("difficultyCeiling")))
        assertEquals(0, cursorProf.getInt(cursorProf.getColumnIndexOrThrow("hasSeenWorkoutTour")))
        assertEquals(45, cursorProf.getInt(cursorProf.getColumnIndexOrThrow("workoutLengthMinutes")))
        cursorProf.close()
    }
}