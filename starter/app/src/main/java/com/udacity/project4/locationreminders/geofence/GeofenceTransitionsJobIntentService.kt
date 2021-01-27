package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.asReminderDataItem
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.geofenceErrorOf
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == GeofencingConstants.SAVE_GEOFENCE_ACTION) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (geofencingEvent.hasError()) {
                val error = geofenceErrorOf(this, geofencingEvent.errorCode)
                Timber.d(error)
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val triggeringGeofences = geofencingEvent.triggeringGeofences
                if (triggeringGeofences.isNotEmpty()) {
                    sendNotification(triggeringGeofences)
                }
            }
        }
    }

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        val requestId = triggeringGeofences[0].requestId

        val remindersLocalRepository: ReminderDataSource by inject()

        CoroutineScope(coroutineContext).launch(SupervisorJob()) {
            val result = remindersLocalRepository.getReminder(requestId)
            if (result is Result.Success<ReminderDTO>) {
                val reminderDTO = result.data

                sendNotification(
                    this@GeofenceTransitionsJobIntentService, reminderDTO.asReminderDataItem()
                )
            }
        }
    }

}