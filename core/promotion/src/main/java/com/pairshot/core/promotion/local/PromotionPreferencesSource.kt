package com.pairshot.core.promotion.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pairshot.core.promotion.domain.PromotionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.promotionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "promotion_state",
)

@Singleton
class PromotionPreferencesSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = false
            }

        private object Keys {
            val MEMBERSHIP_JSON = stringPreferencesKey("membership_json")
            val PENDING_CODE = stringPreferencesKey("pending_code")
            val PENDING_SINCE = longPreferencesKey("pending_since")
        }

        val state: Flow<PromotionState> =
            context.promotionDataStore.data.map { prefs ->
                val raw = prefs[Keys.MEMBERSHIP_JSON] ?: return@map PromotionState.Empty
                runCatching { json.decodeFromString<PromotionState>(raw) }
                    .getOrElse {
                        Timber.tag(TAG).w(it, "Stored membership decode failed — treating as empty")
                        PromotionState.Empty
                    }
            }

        val pending: Flow<PendingPromotionActivation?> =
            context.promotionDataStore.data.map { prefs ->
                val code = prefs[Keys.PENDING_CODE] ?: return@map null
                val since = prefs[Keys.PENDING_SINCE] ?: return@map null
                PendingPromotionActivation(code = code, sinceEpochMillis = since)
            }

        suspend fun save(state: PromotionState) {
            val encoded = json.encodeToString(PromotionState.serializer(), state)
            context.promotionDataStore.edit { prefs ->
                prefs[Keys.MEMBERSHIP_JSON] = encoded
            }
        }

        suspend fun savePending(
            code: String,
            sinceEpochMillis: Long,
        ) {
            context.promotionDataStore.edit { prefs ->
                prefs[Keys.PENDING_CODE] = code
                prefs[Keys.PENDING_SINCE] = sinceEpochMillis
            }
        }

        suspend fun clearPending() {
            context.promotionDataStore.edit { prefs ->
                prefs.remove(Keys.PENDING_CODE)
                prefs.remove(Keys.PENDING_SINCE)
            }
        }

        suspend fun clear() {
            context.promotionDataStore.edit { prefs -> prefs.clear() }
        }

        private companion object {
            const val TAG = "PromotionPrefs"
        }
    }
