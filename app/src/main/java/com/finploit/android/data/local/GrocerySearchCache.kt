package com.finploit.android.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finploit.android.data.dto.GrocerySearchResponseDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TTL_MS = 86_400_000L // 24 hours

@Singleton
class GrocerySearchCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val gson = Gson()
    private val KEY = stringPreferencesKey("grocery_search_cache_v1")

    private data class CacheEntry(val timestamp: Long, val data: GrocerySearchResponseDto)

    fun buildKey(query: String, supermarkets: List<String>?, postalCode: String?): String =
        "${query.trim().lowercase()}|${supermarkets?.sorted()?.joinToString(",") ?: ""}|${postalCode ?: ""}"

    suspend fun get(key: String): GrocerySearchResponseDto? {
        val raw = dataStore.data.first()[KEY] ?: return null
        return try {
            val type = object : TypeToken<Map<String, CacheEntry>>() {}.type
            val map: Map<String, CacheEntry> = gson.fromJson(raw, type) ?: return null
            val entry = map[key] ?: return null
            if (System.currentTimeMillis() - entry.timestamp > TTL_MS) null else entry.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun put(key: String, data: GrocerySearchResponseDto) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY]
            val existing: MutableMap<String, CacheEntry> = try {
                val type = object : TypeToken<MutableMap<String, CacheEntry>>() {}.type
                gson.fromJson(raw, type) ?: mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
            val now = System.currentTimeMillis()
            // Prune expired entries to keep DataStore lean
            existing.entries.removeIf { now - it.value.timestamp > TTL_MS }
            existing[key] = CacheEntry(timestamp = now, data = data)
            prefs[KEY] = gson.toJson(existing)
        }
    }
}
