package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        remindersLocalRepository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun shouldGetReminderById() = runBlocking {
        val reminder = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)

        remindersLocalRepository.saveReminder(reminder)

        val result = remindersLocalRepository.getReminder(reminder.id)

        MatcherAssert.assertThat(result.succeeded, `is`(true))

        result as Result.Success<ReminderDTO>

        MatcherAssert.assertThat(result.data, `is`(notNullValue()))
        MatcherAssert.assertThat(result.data.id, `is`(reminder.id))
        MatcherAssert.assertThat(result.data.title, `is`(reminder.title))
        MatcherAssert.assertThat(result.data.description, `is`(reminder.description))
        MatcherAssert.assertThat(result.data.location, `is`(reminder.location))
    }

    @Test
    fun shouldSaveAndGetAllReminders() = runBlocking {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.saveReminder(reminder2)

        val result = remindersLocalRepository.getReminders()

        MatcherAssert.assertThat(result.succeeded, `is`(true))

        result as Result.Success<List<ReminderDTO>>

        MatcherAssert.assertThat(result.data.size, `is`(2))
        MatcherAssert.assertThat(result.data, hasItem(reminder1))
        MatcherAssert.assertThat(result.data, hasItem(reminder2))
    }

    @Test
    fun shouldDeleteAllReminders() = runBlocking {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        remindersLocalRepository.saveReminder(reminder1)
        remindersLocalRepository.saveReminder(reminder2)

        remindersLocalRepository.deleteAllReminders()

        val result = remindersLocalRepository.getReminders()

        MatcherAssert.assertThat(result.succeeded, `is`(true))

        result as Result.Success<List<ReminderDTO>>

        MatcherAssert.assertThat(result.data, `is`(emptyList()))
    }
}