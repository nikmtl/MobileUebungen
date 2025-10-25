package com.example.mobileuebungen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() ?: false) {
            Log.e("GeofenceReceiver", "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent?.geofenceTransition
        val triggeredIds = geofencingEvent?.triggeringGeofences?.map { it.requestId } ?: emptyList()

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d("GeofenceReceiver", "ENTER geofence: $triggeredIds")
            }
            else -> Log.d("GeofenceReceiver", "Unhanded geofence transition: $transition for $triggeredIds")
        }
    }
}

