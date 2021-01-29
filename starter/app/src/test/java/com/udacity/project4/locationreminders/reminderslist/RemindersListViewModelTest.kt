package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RemindersListViewModelTest : AutoCloseKoinTest() {

    private lateinit var viewModel: RemindersListViewModel

    private lateinit var reminderDataSource: FakeDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        reminderDataSource = FakeDataSource()

        viewModel = RemindersListViewModel(getApplicationContext(), reminderDataSource)
    }

    @Test
    fun shouldLoadReminders_GivenNotEmptyDataSource() = mainCoroutineRule.runBlockingTest {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        reminderDataSource.reminders[reminder1.id] = reminder1
        reminderDataSource.reminders[reminder2.id] = reminder2

        viewModel.loadReminders()

        val reminders = viewModel.remindersList.getOrAwaitValue()

        assertThat(reminders, `is`(notNullValue()))
        assertThat(reminders.size, `is`(reminderDataSource.reminders.size))
        assertThat(reminders.any { it.id == reminder1.id }, `is`(true))
        assertThat(reminders.any { it.id == reminder2.id }, `is`(true))
    }

    @Test
    fun shouldShowNoData_GivenEmptyDataSource() = mainCoroutineRule.runBlockingTest {
        viewModel.loadReminders()

        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }


    @Test
    fun shouldSHowErrorMessage_OnLoadRemindersError() = mainCoroutineRule.runBlockingTest {
        reminderDataSource.shouldReturnError = true

        viewModel.loadReminders()

        assertThat(viewModel.showErrorMessage.getOrAwaitValue(), `is`(notNullValue()))
    }

    @Test
    fun shouldToggleLoading_OnLoadReminders() = mainCoroutineRule.runBlockingTest {
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}