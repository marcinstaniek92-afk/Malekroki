package com.malekroki.app.data

import com.malekroki.app.domain.AppData
import com.malekroki.app.domain.backup.BackupDto
import com.malekroki.app.domain.backup.BackupMapper
import com.malekroki.app.domain.backup.BackupValidation
import com.malekroki.app.domain.backup.BackupValidator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object BackupJson {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: AppData, exportedAt: String): String =
        json.encodeToString(BackupDto.serializer(), BackupMapper.toDto(data, exportedAt))

    fun decodeAndValidate(text: String): BackupValidation {
        if (text.isBlank()) return BackupValidation.Invalid(listOf("Plik jest pusty."))
        val dto = try {
            json.decodeFromString(BackupDto.serializer(), text)
        } catch (e: SerializationException) {
            return BackupValidation.Invalid(listOf("Plik nie jest poprawną kopią zapasową (błędny JSON)."))
        } catch (e: IllegalArgumentException) {
            return BackupValidation.Invalid(listOf("Plik nie jest poprawną kopią zapasową."))
        }
        return BackupValidator.validate(dto)
    }
}
