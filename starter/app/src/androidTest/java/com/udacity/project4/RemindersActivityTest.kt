package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 18)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var device: UiDevice

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun initializeKoin() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao("locationReminders.test.db", appContext) }
        }

        startKoin {
            modules(listOf(myModule))
        }

        repository = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun initializeIdlingResources() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    /**
     * E2E test for adding a reminder. The journey starts from the RemindersActivity.
     */
    @Test
    fun testAddReminderJourney() {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        // Check in ReminderListFragment
        onView(withId(R.id.remindersRecyclerView)).check(matches(isDisplayed()))

        // Go to SaveReminderFragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Fill title
        onView(withId(R.id.reminderTitle))
            .check(matches(isDisplayed()))
            .perform(typeText("TITLE"))

        // Fill description
        onView(withId(R.id.reminderDescription))
            .check(matches(isDisplayed()))
            .perform(typeText("DESCRIPTION"))

        // Go to POI selection
        onView(withId(R.id.selectLocation))
            .check(matches(isDisplayed()))
            .perform(click())

        // Use uiautomator to long click on center of map
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.desc("Google Map")), 1000)

        val maps =
            device.findObject(UiSelector().descriptionContains(appContext.getString(R.string.google_maps_poi_selection)))

        //long clicks the center of the map
        maps.click()

        // Select location
        onView(withId(R.id.selectLocationBtn)).perform(click())

        // Check selected location
        device.wait(Until.hasObject(By.desc(appContext.getString(R.string.reminder_location_label))), 1000)
        onView(withId(R.id.selectedLocation)).check(matches(not(withText(""))))

        // Save reminder
        onView(withId(R.id.saveReminder)).perform(click())

        // Check reminder appear in recycler view
        onView(withId(R.id.remindersRecyclerView))
            .check(matches(hasDescendant(not(withText("")))))
    }

}
