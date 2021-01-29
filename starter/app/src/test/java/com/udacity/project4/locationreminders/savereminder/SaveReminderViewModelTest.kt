package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class SaveReminderViewModelTest : AutoCloseKoinTest() {

    private lateinit var viewModel: SaveReminderViewModel

    private lateinit var reminderDataSource: ReminderDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        reminderDataSource = FakeDataSource()

        viewModel = SaveReminderViewModel(getApplicationContext(), reminderDataSource)
    }

    @Test
    fun shouldSaveReminder() = mainCoroutineRule.runBlockingTest {
        val reminder = ReminderDataItem("TITLE", "DESCRIPTION", "LOCATION", 0.0, 0.0)
        viewModel.saveReminder(reminder)

        val result = reminderDataSource.getReminder(reminder.id)

        assertThat(result, `is`(notNullValue()))
        assertThat(result.succeeded, `is`(true))
        result as Success<ReminderDTO>

        assertThat(result.data.id, `is`(reminder.id))
    }

    @Test
    fun shouldReturnTrue_OnValidateReminder_GivenLegal() {
        val reminder = ReminderDataItem("TITLE", "DESCRIPTION", "LOCATION", 0.0, 0.0)
        assertThat(viewModel.validateEnteredData(reminder), `is`(true))
    }

    @Test
    fun shouldShowTitleError_OnValidateReminder_GivenEmptyTitle() {
        val reminder = ReminderDataItem("", "DESCRIPTION", "LOCATION", 0.0, 0.0)

        assertThat(viewModel.validateEnteredData(reminder), `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun shouldShowTitleError_OnValidateReminder_GivenNullTitle() {
        val reminder = ReminderDataItem(null, "DESCRIPTION", "LOCATION", 0.0, 0.0)

        assertThat(viewModel.validateEnteredData(reminder), `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun shouldShowTitleError_OnValidateReminder_GivenEmptyLocation() {
        val reminder = ReminderDataItem("TITLE", "DESCRIPTION", "", 0.0, 0.0)

        assertThat(viewModel.validateEnteredData(reminder), `is`(false))
        assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun shouldShowTitleError_OnValidateReminder_GivenNullLocation() {
        val reminder = ReminderDataItem("TITLE", "DESCRIPTION", null, 0.0, 0.0)

        assertThat(viewModel.validateEnteredData(reminder), `is`(false))
        assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun shouldSetLocationAndLatLong_OnSetSelectedPoi() {
        val poi = PointOfInterest(LatLng(1.0, 2.0), "PLACE_ID", "NAME")
        viewModel.setSelectedPoi(poi)

        assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`("NAME"))
        assertThat(viewModel.latitude.getOrAwaitValue(), `is`(poi.latLng.latitude))
        assertThat(viewModel.longitude.getOrAwaitValue(), `is`(poi.latLng.longitude))
    }

    @Test
    fun shouldClearReminderLiveData() {
        viewModel.onClear()

        assertThat(viewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
        assertThat(viewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
    }
}
