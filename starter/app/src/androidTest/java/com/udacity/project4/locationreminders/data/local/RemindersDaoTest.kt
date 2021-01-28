package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var remindersDao: RemindersDao

    private lateinit var database: RemindersDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        remindersDao = database.reminderDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun shouldGetReminderById() = runBlockingTest {
        val reminder = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)

        remindersDao.saveReminder(reminder)

        val result = remindersDao.getReminderById(reminder.id)

        assertThat(result as ReminderDTO, `is`(notNullValue()))
        assertThat(result.id, `is`(reminder.id))
        assertThat(result.title, `is`(reminder.title))
        assertThat(result.description, `is`(reminder.description))
        assertThat(result.location, `is`(reminder.location))
    }

    @Test
    fun shouldSaveAndGetAllReminders() = runBlockingTest {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        remindersDao.saveReminder(reminder1)
        remindersDao.saveReminder(reminder2)

        val result = remindersDao.getReminders()

        assertThat(result.size, `is`(2))
        assertThat(result, hasItem(reminder1))
        assertThat(result, hasItem(reminder2))
    }

    @Test
    fun shouldDeleteAllReminders() = runBlockingTest{
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        remindersDao.saveReminder(reminder1)
        remindersDao.saveReminder(reminder2)

        remindersDao.deleteAllReminders()

        val reminders = remindersDao.getReminders()

        assertThat(reminders, `is`(emptyList()))
    }
}