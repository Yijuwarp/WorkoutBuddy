package com.example.workoutbuddy.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// A user-named snapshot of an equipment set (e.g. "Home", "Gym") that can be re-applied in one
// tap instead of re-toggling each piece of equipment by hand.
@Entity(tableName = "equipment_presets")
data class EquipmentPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val equipmentCsv: String // CSV of Equipment ids
)
