package com.example.boardgamerapp.data.local

import androidx.room.TypeConverter
import com.example.boardgamerapp.domain.model.GameNightStatus
import java.time.LocalDateTime

class DatabaseConverters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromGameNightStatus(value: GameNightStatus?): String? = value?.name

    @TypeConverter
    fun toGameNightStatus(value: String?): GameNightStatus? =
        value?.let(GameNightStatus::valueOf)
}
