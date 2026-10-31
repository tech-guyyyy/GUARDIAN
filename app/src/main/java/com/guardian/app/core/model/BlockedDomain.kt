package com.guardian.app.core.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_domains",
    indices = [Index(value = ["normalizedDomain"], unique = true)]
)
data class BlockedDomain(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawInput: String,
    val normalizedDomain: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isCustom: Boolean = true
) {
    companion object {
        fun normalize(input: String): String {
            var cleaned = input.trim().lowercase()
            if (cleaned.startsWith("http://")) cleaned = cleaned.removePrefix("http://")
            if (cleaned.startsWith("https://")) cleaned = cleaned.removePrefix("https://")
            if (cleaned.startsWith("www.")) cleaned = cleaned.removePrefix("www.")
            val slashIdx = cleaned.indexOf('/')
            if (slashIdx != -1) {
                cleaned = cleaned.substring(0, slashIdx)
            }
            val colonIdx = cleaned.indexOf(':')
            if (colonIdx != -1) {
                cleaned = cleaned.substring(0, colonIdx)
            }
            return cleaned
        }
    }
}
