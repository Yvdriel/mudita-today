package com.mosquishe.today.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User settings: checklist auto-complete + when the day starts. Backed by Preferences DataStore. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val AUTO_COMPLETE = booleanPreferencesKey("auto_complete_when_checklist_done")
        val DAY_START_MINUTE = intPreferencesKey("day_start_minute_of_day")
    }

    /** Complete a to-do automatically once every checklist item is done. Default on. */
    val autoComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_COMPLETE] ?: DEFAULT_AUTO_COMPLETE }

    /** Minute-of-day at which a new day's to-dos start appearing. Default 03:00 (= 180). */
    val dayStartMinuteOfDay: Flow<Int> =
        context.dataStore.data.map { it[Keys.DAY_START_MINUTE] ?: DEFAULT_DAY_START_MINUTE }

    /** [dayStartMinuteOfDay] as a [LocalTime]. */
    val dayStart: Flow<LocalTime> =
        dayStartMinuteOfDay.map { LocalTime.of(it / 60, it % 60) }

    suspend fun autoCompleteValue(): Boolean = autoComplete.first()
    suspend fun dayStartValue(): LocalTime = dayStart.first()

    suspend fun setAutoComplete(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_COMPLETE] = enabled }
    }

    suspend fun setDayStartMinuteOfDay(minuteOfDay: Int) {
        context.dataStore.edit { it[Keys.DAY_START_MINUTE] = minuteOfDay.coerceIn(0, 24 * 60 - 1) }
    }

    companion object {
        const val DEFAULT_AUTO_COMPLETE = true
        const val DEFAULT_DAY_START_MINUTE = 180 // 03:00
    }
}
