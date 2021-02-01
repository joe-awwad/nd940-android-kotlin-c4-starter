package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.asReminderDataItem
import com.udacity.project4.locationreminders.data.local.LocalDB
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest(), KoinComponent {

    private lateinit var dataSource: FakeAndroidDataSource

    private lateinit var viewModel: RemindersListViewModel

    private lateinit var appContext: Application

    @Before
    fun setUp() {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()
        val testModule = module {
            viewModel {
                RemindersListViewModel(appContext, get() as ReminderDataSource)
            }
            single<ReminderDataSource> { FakeAndroidDataSource() } bind (FakeAndroidDataSource::class)
            single { LocalDB.createRemindersDao(appContext) }
        }

        startKoin {
            modules(listOf(testModule))
        }

        dataSource = get()

        viewModel = get()

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    @Test
    fun shouldNavigateToAddReminder_OnFabButtonClicked() = runBlockingTest {
        val navController = mock(NavController::class.java)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        onView(withId(R.id.addReminderFAB))
            .perform(click())

        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun shouldNavigateToSelectedReminderDetails() = runBlockingTest {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        dataSource.saveReminder(reminder1)
        dataSource.saveReminder(reminder2)

        val navController = mock(NavController::class.java)

        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        onView(withId(R.id.remindersRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText(reminder2.title)), click()
                )
            )

        verify(navController).navigate(
            ReminderListFragmentDirections.toReminderDescriptionActivity(
                reminder2.asReminderDataItem()
            )
        )
    }

    @Test
    fun shouldContain2Reminders() = runBlockingTest {
        val reminder1 = ReminderDTO("TITLE1", "DESCRIPTION1", "LOCATION1", 0.0, 0.0)
        val reminder2 = ReminderDTO("TITLE2", "DESCRIPTION2", "LOCATION2", 0.0, 0.0)

        dataSource.saveReminder(reminder1)
        dataSource.saveReminder(reminder2)

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(withId(R.id.remindersRecyclerView))
            .check(matches(hasDescendant(withText(reminder1.title))))
            .check(matches(hasDescendant(withText(reminder1.description))))
            .check(matches(hasDescendant(withText(reminder1.location))))

        onView(withId(R.id.remindersRecyclerView))
            .check(matches(hasDescendant(withText(reminder2.title))))
            .check(matches(hasDescendant(withText(reminder2.description))))
            .check(matches(hasDescendant(withText(reminder2.location))))
    }

    @Test
    fun shouldShowErrorToast_OnDataSourceError() {
        dataSource.shouldReturnError = true

        var frag: ReminderListFragment? = null
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment {
            frag = it
        }

        assertThat(frag, `is`(notNullValue()))

        frag?.let {
            onView(withText(any(String::class.java))).inRoot(
                withDecorView(
                    not(`is`(it.requireActivity().window.decorView))
                )
            ).check(matches(isDisplayed()))
        }
    }


}