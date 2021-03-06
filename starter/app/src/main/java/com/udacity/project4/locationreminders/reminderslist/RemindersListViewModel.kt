package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.GeofencingClient
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class RemindersListViewModel(
    val app: Application,
    private val dataSource: ReminderDataSource
) : BaseViewModel(app) {

    val remindersList = MutableLiveData<List<ReminderDataItem>>()

    private val geofencingClient: GeofencingClient by lazy {
        GeofencingClient(app.applicationContext)
    }

    fun navigateToReminderDetails(dataItem: ReminderDataItem) {
        navigationCommand.value =
            NavigationCommand.To(
                ReminderListFragmentDirections.toReminderDescriptionActivity(
                    dataItem
                )
            )
    }

    fun navigateToAuthenticationActivity() {
        navigationCommand.value =
            NavigationCommand.To(ReminderListFragmentDirections.toAuthenticationActivity())
    }


    fun navigateToSaveReminder(){
        navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }
    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */
    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            //interacting with the dataSource has to be through a coroutine
            val result = dataSource.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                        //map the reminder data from the DB to the be ready to be displayed on the UI
                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    remindersList.value = dataList
                }
                is Result.Error -> {
                    showErrorMessage.value = result.message!!
                }
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {

            when (val result = dataSource.getReminders()) {
                is Result.Success -> {
                    val geofenceIds = result.data.map { it.id }
                    if (geofenceIds.isNotEmpty()) {
                        geofencingClient.removeGeofences(geofenceIds)
                            .addOnFailureListener { e ->
                                Timber.d(e, "Failed to remove reminder geofences")
                            }
                            .addOnSuccessListener {
                                clearAllRemindersFromLocalDb()
                            }
                    }
                }
                else -> Timber.d("Failed to remove reminder geofences")
            }
        }
    }

    private fun clearAllRemindersFromLocalDb() {
        if (!remindersList.value.isNullOrEmpty()) {
            viewModelScope.launch {
                doWhileLoading {
                    withContext(Dispatchers.IO) {
                        dataSource.deleteAllReminders()
                        showToast.postValue(app.getString(R.string.reminders_cleared))
                    }
                }
                loadReminders()
            }
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = remindersList.value == null || remindersList.value!!.isEmpty()
    }

    private suspend fun doWhileLoading(block: suspend () -> Unit) {
        showLoading.postValue(true)

        block()

        showLoading.postValue(false)
    }
}
