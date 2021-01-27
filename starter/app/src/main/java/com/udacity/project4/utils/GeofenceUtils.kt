package com.udacity.project4.utils

import android.content.Context
import com.google.android.gms.location.GeofenceStatusCodes
import com.udacity.project4.R

// Code taken from android-kotlin-geo-fences app provided in Udacity's Kotlin Android Developer Nano-degree

/**
 * Returns the error string for a geofencing error code.
 */
fun geofenceErrorOf(context: Context, errorCode: Int): String {
    val resources = context.resources
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_not_available
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_too_many_geofences
        )
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_too_many_pending_intents
        )
        else -> resources.getString(R.string.error_adding_geofence)
    }
}

internal object GeofencingConstants {

    const val GEOFENCE_RADIUS_IN_METERS = 100f

    const val SAVE_GEOFENCE_ACTION = "Action.SaveGeofence.LocationReminder"
}