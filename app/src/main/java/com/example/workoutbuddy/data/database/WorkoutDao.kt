package com.example.workoutbuddy.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Exercise Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertExercise(exercise: ExerciseEntity): Long

    @Query("SELECT COUNT(*) FROM exercises")
    fun getExerciseCount(): Int

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE category = :category")
    fun getExercisesByCategory(category: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getExerciseById(id: Int): ExerciseEntity?

    // Workout Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    fun updateWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    fun deleteWorkout(workoutId: Long)

    @Query("SELECT * FROM workouts WHERE isCompleted = 0 LIMIT 1")
    fun getActiveWorkoutDraft(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE isCompleted = 1 ORDER BY date DESC LIMIT 5")
    fun getLast5CompletedWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE isCompleted = 1 ORDER BY date DESC LIMIT 100")
    fun getLast100CompletedWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE isCompleted = 1 ORDER BY date DESC")
    fun getAllCompletedWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutById(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE isCompleted = 1 AND date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getCompletedWorkoutsForDate(startOfDay: Long, endOfDay: Long): List<WorkoutEntity>

    // Workout Set Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkoutSet(set: WorkoutSetEntity): Long

    @Update
    fun updateWorkoutSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    fun deleteWorkoutSet(setId: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    fun deleteWorkoutSetsForWorkout(workoutId: Long)

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY setNumber ASC")
    fun getSetsForWorkout(workoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY setNumber ASC")
    fun getSetsForWorkoutFlow(workoutId: Long): Flow<List<WorkoutSetEntity>>

    // PR and History Queries
    @Query("SELECT MAX(s.weight) FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1")
    fun getBestWeightForExercise(exerciseId: Int): Double?

    @Query("SELECT MAX(s.time) FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1")
    fun getBestTimeForExercise(exerciseId: Int): Int?

    @Query("SELECT MAX(s.distance) FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1")
    fun getBestDistanceForExercise(exerciseId: Int): Double?

    // Gets all sets of an exercise from the most recent workout where it was performed
    @Query("SELECT s.* FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND w.isCompleted = 1 AND s.workoutId = (SELECT s2.workoutId FROM workout_sets s2 INNER JOIN workouts w2 ON s2.workoutId = w2.id WHERE s2.exerciseId = :exerciseId AND s2.isCompleted = 1 AND w2.isCompleted = 1 ORDER BY s2.id DESC LIMIT 1) ORDER BY s.setNumber ASC")
    fun getPreviousWorkoutSetsForExercise(exerciseId: Int): List<WorkoutSetEntity>

    @Query("SELECT s.* FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1 ORDER BY s.weight DESC, s.reps DESC LIMIT 1")
    fun getBestSetForExercise(exerciseId: Int): WorkoutSetEntity?

    @Query("SELECT s.* FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1 ORDER BY s.distance DESC, COALESCE(s.inclinePct, 0.0) DESC, s.time ASC LIMIT 1")
    fun getBestDistanceSetForExercise(exerciseId: Int): WorkoutSetEntity?

    @Query("SELECT s.* FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1 ORDER BY s.time DESC LIMIT 1")
    fun getBestTimeSetForExercise(exerciseId: Int): WorkoutSetEntity?

    @Query("SELECT COUNT(DISTINCT s.workoutId) FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1")
    fun getCompletedWorkoutCountForExercise(exerciseId: Int): Int

    // Per-exercise usage stats (times logged + most recent use) for the exercise picker's
    // "Most Logged" / "Recent" sort modes, computed in one pass rather than N+1 per exercise.
    @Query("SELECT s.exerciseId AS exerciseId, COUNT(*) AS logCount, MAX(w.date) AS lastUsedDate FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.isCompleted = 1 AND w.isCompleted = 1 GROUP BY s.exerciseId")
    fun getExerciseUsageStats(): List<ExerciseUsageStat>

    // User Profile Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    // Exercise Preference Queries
    @Query("SELECT * FROM exercise_preferences")
    fun getAllPreferences(): Flow<List<ExercisePreferenceEntity>>

    @Query("SELECT * FROM exercise_preferences")
    fun getAllPreferencesOnce(): List<ExercisePreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPreference(preference: ExercisePreferenceEntity)

    @Query("DELETE FROM exercise_preferences WHERE exerciseId = :exerciseId")
    fun deletePreference(exerciseId: Int)

    // Equipment Preset Queries
    @Query("SELECT * FROM equipment_presets ORDER BY id ASC")
    fun getAllEquipmentPresets(): Flow<List<EquipmentPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEquipmentPreset(preset: EquipmentPresetEntity): Long

    @Query("DELETE FROM equipment_presets WHERE id = :id")
    fun deleteEquipmentPreset(id: Int)

    // Muscle Group Recovery Queries
    @Query("SELECT * FROM muscle_group_recovery")
    fun getAllRecoveryFlow(): Flow<List<MuscleGroupRecoveryEntity>>

    @Query("SELECT * FROM muscle_group_recovery WHERE muscleGroup = :muscleGroup")
    fun getRecovery(muscleGroup: String): MuscleGroupRecoveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertRecovery(recovery: MuscleGroupRecoveryEntity)

    // Distinct exercises the user has logged at least one completed set for, used by the Body
    // tab's Results list to only show exercises that "have been done."
    @Query("SELECT DISTINCT s.exerciseId FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.isCompleted = 1 AND w.isCompleted = 1")
    fun getLoggedExerciseIds(): List<Int>

    // All completed sets for an exercise, oldest workout first, used to derive a trend
    // (best-per-session across the last few sessions) for the Body tab's Results list.
    @Query("SELECT s.* FROM workout_sets s INNER JOIN workouts w ON s.workoutId = w.id WHERE s.exerciseId = :exerciseId AND s.isCompleted = 1 AND w.isCompleted = 1 ORDER BY w.date ASC, s.id ASC")
    fun getCompletedSetsForExerciseOrdered(exerciseId: Int): List<WorkoutSetEntity>
}
