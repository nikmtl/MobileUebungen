package com.example.mobileuebungen

import RaplaParser
import RaplaResult
import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import java.time.LocalDate


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val volleyRequestQueue =
            Volley.newRequestQueue(this) // request queue for http calls with volley


        enableEdgeToEdge()
        setContent {
            var status by remember { mutableStateOf(Status.READY) }
            val url =
                "https://rapla.dhbw.de/rapla/calendar?key=SF8qHSuYFD3SStyfcj4vvmAhUMdwoDn7AYC1DTtyyBmhFJAv8m_hIYVHpm9Ul6nMjqX11N94dkWx78kCdoJxR44ru1kegzIBOMCCSJVRikkSTGNCV0YyThLBR30y9hOaGryjvwt1kpad5g93Dkdn0A&salt=-218630611"
            var raplaResult by remember { mutableStateOf<RaplaResult?>(null) }

            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Row( // Top Row with Title
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Upcoming Events",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )
                }
                Column( // Event List
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val events = raplaResult?.weeks?.flatMap { it.events }
                        ?.filter { LocalDate.now() <= it.date }?.sortedBy { it.date }
                        ?: emptyList()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(events) { event ->
                            EventListItem(
                                event.title,
                                event.course,
                                event.date,
                                event.startTime,
                                event.endTime,
                                event.room ?: "No location specified",
                            )
                        }
                    }
                }
                //Lower Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(7.dp)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    if (status != Status.SUCCESS) {
                        Button(
                            onClick = {
                                status = Status.FETCHING
                                val stringRequest = StringRequest(Request.Method.GET, url, { html ->
                                    val raplaParser = RaplaParser()
                                    raplaResult = raplaParser.parse(html)
                                    Log.d(
                                        "MainActivity",
                                        "Parsed Event Titles: ${raplaResult?.allEventTitles()}"
                                    )
                                    status = Status.SUCCESS
                                }, { error ->
                                    status = Status.ERROR
                                    Log.e("MainActivity", "Error fetching data: $error")
                                })
                                volleyRequestQueue.add(stringRequest)
                            },
                            enabled = status != Status.FETCHING
                        ) {
                            Text(
                                when (status) {
                                    Status.READY -> "Fetch Rapla Events"
                                    Status.FETCHING -> "Fetching..."
                                    Status.ERROR -> "Fetch failed. Retry?"
                                    Status.SUCCESS -> "Refetch Rapla Events"
                                }
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                requestCalendarAccess()
                            }
                        ) { Text("Add events to Calendar") }
                    }

                }
            }
        }
    }

    private fun initializeGeofencing() {
        val geofencingClient = LocationServices.getGeofencingClient(this)
        val geofence = Geofence.Builder()
            .setCircularRegion(
                49.4738, 8.5344,
                50f
            )
    }

    fun requestCalendarAccess() { // Warning not woking yet
        val calendarPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {

                permissions.getOrDefault(Manifest.permission.WRITE_CALENDAR, false) -> {
                    Log.d("MainActivity", "Write access granted.")
                }

                permissions.getOrDefault(Manifest.permission.READ_CALENDAR, false) -> {
                    Log.d("MainActivity", "Read access granted.")
                }

                else -> {
                    Log.d("MainActivity", "No calendar access granted.")
                }
            }
        }

        calendarPermissionRequest.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )

    }
}
