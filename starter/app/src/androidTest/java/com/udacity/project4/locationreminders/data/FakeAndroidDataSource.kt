package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

/**
 * FakeDataSource: test double to the LocalDataSource
 */
class FakeAndroidDataSource : ReminderDataSource {

    var reminders = LinkedHashMap<String, ReminderDTO>()

    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Could not get reminders")
        }

        return Result.Success(reminders.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Could not get reminder $id")
        }

        reminders[id]?.let {
            return Result.Success(it)
        }

        return Result.Error("Could not find reminder: $id")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}