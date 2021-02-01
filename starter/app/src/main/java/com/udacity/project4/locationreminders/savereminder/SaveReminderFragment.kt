package com.udacity.project4.locationreminders.savereminder

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.GeofencingConstants.SAVE_GEOFENCE_ACTION
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private lateinit var geofencingClient: GeofencingClient

    private lateinit var geofencePendingIntent: PendingIntent

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            _viewModel.getReminderDataItem().let {
                if (_viewModel.validateEnteredData(it)) {

                    checkOrRequestLocationPermissions()

                    checkDeviceLocationIsOnAndAddGeofence(it)

                    _viewModel.saveReminder(it)
                }
            }

        }

        initializeGeofencingPendingIntent()
    }

    private fun initializeGeofencingPendingIntent() {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            .apply {
                action = SAVE_GEOFENCE_ACTION
            }
        geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TURN_DEVICE_LOCATION_ON_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            addGeofenceForReminder(_viewModel.getReminderDataItem())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode.isForegroundAndBackgroundPermissionRequestCode()
            || requestCode.isForegroundPermissionRequestCode()
        ) {
            if (grantResults.isEmpty() || grantResults.any { it == PERMISSION_DENIED }) {
                Snackbar.make(
                    binding.saveReminderMain,
                    R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        displayAppSettings()
                    }.show()
            }
        } else {
            _viewModel.geofencingActivated()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkOrRequestLocationPermissions() {
        if (_viewModel.isGeofencingActive()) return
        if (isLocationPermissionsGranted()) return
        requestLocationPermissions()
    }

    private fun displayAppSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Suppress("DEPRECATION")
    private fun requestLocationPermissions() {
        if (runningQOrLater) {
            if (!isBackgroundAndForegroundPermissionGranted()) {
                requestPermissions(
                    arrayOf(
                        ACCESS_FINE_LOCATION,
                        ACCESS_BACKGROUND_LOCATION
                    ),
                    FOREGROUND_AND_BACKGROUND_PERMISSIONS_REQUEST_CODE
                )
            }
        } else {
            if (!isForegroundPermissionGranted()) {
                requestPermissions(
                    arrayOf(ACCESS_FINE_LOCATION),
                    FOREGROUND_ONLY_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isLocationPermissionsGranted(): Boolean {
        return if (runningQOrLater) {
            isBackgroundAndForegroundPermissionGranted()
        } else {
            isForegroundPermissionGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isBackgroundAndForegroundPermissionGranted(): Boolean {
        return isBackgroundPermissionGranted() && isForegroundPermissionGranted()
    }

    private fun isForegroundPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isBackgroundPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            ACCESS_BACKGROUND_LOCATION
        ) == PERMISSION_GRANTED
    }

    private fun checkDeviceLocationIsOnAndAddGeofence(
        reminderData: ReminderDataItem,
        resolve: Boolean = true
    ) {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(
                LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER })
            .build()

        val locationSettings = LocationServices.getSettingsClient(requireContext())
        locationSettings.checkLocationSettings(locationSettingsRequest)
            .addOnFailureListener { e ->
                tryResolveApiError(e, resolve)
            }
            .addOnSuccessListener {
                addGeofenceForReminder(reminderData)
            }
    }

    private fun tryResolveApiError(e: Exception, resolve: Boolean) {
        if (e is ResolvableApiException && resolve) {
            try {
                e.startResolutionForResult(
                    requireActivity(),
                    TURN_DEVICE_LOCATION_ON_REQUEST_CODE
                )

            } catch (sendIntentError: IntentSender.SendIntentException) {
                Timber.d(e)
                Toast.makeText(
                    requireContext(),
                    "Error resolving location settings: ${sendIntentError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Snackbar.make(
                binding.saveReminderMain,
                R.string.location_required_error,
                Snackbar.LENGTH_LONG
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder(reminder: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnFailureListener {
                Timber.d(it, "Failed to add geofence for $reminder")
            }

    }

    companion object {
        private const val FOREGROUND_AND_BACKGROUND_PERMISSIONS_REQUEST_CODE = 20
        private const val FOREGROUND_ONLY_PERMISSION_REQUEST_CODE = 21
        private const val TURN_DEVICE_LOCATION_ON_REQUEST_CODE = 22
    }

    private fun Int.isForegroundAndBackgroundPermissionRequestCode(): Boolean {
        return this == FOREGROUND_AND_BACKGROUND_PERMISSIONS_REQUEST_CODE
    }

    private fun Int.isForegroundPermissionRequestCode(): Boolean {
        return this == FOREGROUND_ONLY_PERMISSION_REQUEST_CODE
    }
}


