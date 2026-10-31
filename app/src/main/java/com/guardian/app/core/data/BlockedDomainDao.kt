package com.guardian.app.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardian.app.core.model.BlockedDomain
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDomainDao {

    @Query("SELECT * FROM blocked_domains ORDER BY addedTimestamp DESC")
    fun getAllDomainsFlow(): Flow<List<BlockedDomain>>

    @Query("SELECT * FROM blocked_domains")
    suspend fun getAllDomains(): List<BlockedDomain>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_domains WHERE normalizedDomain = :domain LIMIT 1)")
    suspend fun isDomainBlocked(domain: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: BlockedDomain): Long

    @Delete
    suspend fun deleteDomain(domain: BlockedDomain)

    @Query("DELETE FROM blocked_domains WHERE id = :id")
    suspend fun deleteById(id: Long)
}
