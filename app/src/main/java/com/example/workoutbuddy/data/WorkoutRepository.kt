package com.example.workoutbuddy.data

import com.example.workoutbuddy.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        if (workoutDao.getExerciseCount() == 0) {
            workoutDao.insertExercises(DatabaseInitializer.getSeedExercises())
        }
    }

    fun getAllExercises(): Flow<List<ExerciseEntity>> = workoutDao.getAllExercises()

    suspend fun insertExercise(exercise: ExerciseEntity): Long = withContext(Dispatchers.IO) {
        workoutDao.insertExercise(exercise)
    }

    suspend fun getExerciseById(id: Int): ExerciseEntity? = withContext(Dispatchers.IO) {
        workoutDao.getExerciseById(id)
    }

    suspend fun getExercisesByCategory(category: String): List<ExerciseEntity> = withContext(Dispatchers.IO) {
        workoutDao.getExercisesByCategory(category)
    }

    suspend fun getActiveWorkoutDraft(): WorkoutEntity? = withContext(Dispatchers.IO) {
        workoutDao.getActiveWorkoutDraft()
    }

    suspend fun createActiveWorkoutDraft(category: String, startingStrength: Double, startingStamina: Double): WorkoutEntity = withContext(Dispatchers.IO) {
        val newWorkout = WorkoutEntity(
            date = System.currentTimeMillis(),
            category = category,
            isCompleted = false,
            startingStrengthScore = startingStrength,
            startingStaminaScore = startingStamina
        )
        val id = workoutDao.insertWorkout(newWorkout)
        newWorkout.copy(id = id)
    }

    suspend fun getWorkoutById(id: Long): WorkoutEntity? = withContext(Dispatchers.IO) {
        workoutDao.getWorkoutById(id)
    }

    suspend fun updateWorkout(workout: WorkoutEntity) = withContext(Dispatchers.IO) {
        workoutDao.updateWorkout(workout)
    }

    suspend fun deleteWorkout(workoutId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteWorkout(workoutId)
    }

    suspend fun getLast5CompletedWorkouts(): List<WorkoutEntity> = withContext(Dispatchers.IO) {
        workoutDao.getLast5CompletedWorkouts()
    }

    fun getAllCompletedWorkouts(): Flow<List<WorkoutEntity>> = workoutDao.getAllCompletedWorkouts()

    suspend fun getCompletedWorkoutsForDate(startOfDay: Long, endOfDay: Long): List<WorkoutEntity> = withContext(Dispatchers.IO) {
        workoutDao.getCompletedWorkoutsForDate(startOfDay, endOfDay)
    }

    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>) = withContext(Dispatchers.IO) {
        workoutDao.insertWorkoutSets(sets)
    }

    suspend fun insertWorkoutSet(set: WorkoutSetEntity): Long = withContext(Dispatchers.IO) {
        workoutDao.insertWorkoutSet(set)
    }

    suspend fun updateWorkoutSet(set: WorkoutSetEntity) = withContext(Dispatchers.IO) {
        workoutDao.updateWorkoutSet(set)
    }

    suspend fun deleteWorkoutSet(setId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteWorkoutSet(setId)
    }

    suspend fun deleteWorkoutSetsForWorkout(workoutId: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteWorkoutSetsForWorkout(workoutId)
    }

    suspend fun getSetsForWorkout(workoutId: Long): List<WorkoutSetEntity> = withContext(Dispatchers.IO) {
        workoutDao.getSetsForWorkout(workoutId)
    }

    fun getSetsForWorkoutFlow(workoutId: Long): Flow<List<WorkoutSetEntity>> = workoutDao.getSetsForWorkoutFlow(workoutId)

    suspend fun getBestWeightForExercise(exerciseId: Int): Double? = withContext(Dispatchers.IO) {
        workoutDao.getBestWeightForExercise(exerciseId)
    }

    suspend fun getBestTimeForExercise(exerciseId: Int): Int? = withContext(Dispatchers.IO) {
        workoutDao.getBestTimeForExercise(exerciseId)
    }

    suspend fun getBestDistanceForExercise(exerciseId: Int): Double? = withContext(Dispatchers.IO) {
        workoutDao.getBestDistanceForExercise(exerciseId)
    }

    suspend fun getPreviousWorkoutSetsForExercise(exerciseId: Int): List<WorkoutSetEntity> = withContext(Dispatchers.IO) {
        workoutDao.getPreviousWorkoutSetsForExercise(exerciseId)
    }

    suspend fun getBestSetForExercise(exerciseId: Int): WorkoutSetEntity? = withContext(Dispatchers.IO) {
        workoutDao.getBestSetForExercise(exerciseId)
    }

    suspend fun getBestDistanceSetForExercise(exerciseId: Int): WorkoutSetEntity? = withContext(Dispatchers.IO) {
        workoutDao.getBestDistanceSetForExercise(exerciseId)
    }

    suspend fun getBestTimeSetForExercise(exerciseId: Int): WorkoutSetEntity? = withContext(Dispatchers.IO) {
        workoutDao.getBestTimeSetForExercise(exerciseId)
    }

    suspend fun getCompletedWorkoutCountForExercise(exerciseId: Int): Int = withContext(Dispatchers.IO) {
        workoutDao.getCompletedWorkoutCountForExercise(exerciseId)
    }

    suspend fun getExerciseUsageStats(): List<ExerciseUsageStat> = withContext(Dispatchers.IO) {
        workoutDao.getExerciseUsageStats()
    }

    fun getUserProfileFlow(): Flow<UserProfileEntity?> = workoutDao.getUserProfileFlow()

    suspend fun getUserProfile(): UserProfileEntity? = withContext(Dispatchers.IO) {
        workoutDao.getUserProfile()
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        workoutDao.insertUserProfile(profile)
    }

    fun getAllPreferences(): Flow<List<ExercisePreferenceEntity>> = workoutDao.getAllPreferences()

    suspend fun getAllPreferencesOnce(): List<ExercisePreferenceEntity> = withContext(Dispatchers.IO) {
        workoutDao.getAllPreferencesOnce()
    }

    suspend fun upsertPreference(preference: ExercisePreferenceEntity) = withContext(Dispatchers.IO) {
        workoutDao.upsertPreference(preference)
    }

    suspend fun deletePreference(exerciseId: Int) = withContext(Dispatchers.IO) {
        workoutDao.deletePreference(exerciseId)
    }

    fun getAllEquipmentPresets(): Flow<List<EquipmentPresetEntity>> = workoutDao.getAllEquipmentPresets()

    suspend fun insertEquipmentPreset(preset: EquipmentPresetEntity): Long = withContext(Dispatchers.IO) {
        workoutDao.insertEquipmentPreset(preset)
    }

    suspend fun deleteEquipmentPreset(id: Int) = withContext(Dispatchers.IO) {
        workoutDao.deleteEquipmentPreset(id)
    }
}
