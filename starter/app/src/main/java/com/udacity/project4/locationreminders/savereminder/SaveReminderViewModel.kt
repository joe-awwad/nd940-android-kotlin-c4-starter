package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.asReminderDTO
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String>()

    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    val reminderSelectedLocationStr = MutableLiveData<String>()

    private val geofencingActive = MutableLiveData(false)

    fun isGeofencingActive(): Boolean {
        return geofencingActive.value!!
    }

    fun geofencingActivated() {
        geofencingActive.value = true
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        latitude.value = null
        longitude.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(reminderData.asReminderDTO())
            showLoading.value = false
            showToast.value =
                app.getString(R.string.geofence_added_for_location, reminderData.location)
            navigationCommand.value = NavigationCommand.BackTo(R.id.reminderListFragment)
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

        return true
    }

    fun setSelectedPoi(poi: PointOfInterest) {
        reminderSelectedLocationStr.value = poi.name
        latitude.value = poi.latLng.latitude
        longitude.value = poi.latLng.longitude
    }

    internal fun getReminderDataItem(): ReminderDataItem {
        return ReminderDataItem(
            title = reminderTitle.value,
            description = reminderDescription.value,
            location = reminderSelectedLocationStr.value,
            latitude = latitude.value,
            longitude = longitude.value,
        )
    }
}