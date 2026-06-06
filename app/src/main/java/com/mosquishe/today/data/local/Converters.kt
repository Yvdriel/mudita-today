package com.mosquishe.today.data.local

import androidx.room.TypeConverter
import com.mosquishe.today.domain.Recurrence
import java.time.Instant
import java.time.LocalDate

/** Room type converters: dates as epoch-day, instants as epoch-milli, recurrence as its storage string. */
class Converters {
    @TypeConverter fun dateToLong(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)

    @TypeConverter fun instantToLong(i: Instant?): Long? = i?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun recurrenceToString(r: Recurrence?): String? = r?.toStorageString()
    @TypeConverter fun stringToRecurrence(s: String?): Recurrence? = Recurrence.parse(s)
}
