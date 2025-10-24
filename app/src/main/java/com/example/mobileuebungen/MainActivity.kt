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


class MainActivity : ComponentActivity() {
    val locationPermissionReqeust = registerForActivityResult(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val queue = Volley.newRequestQueue(this)

        enableEdgeToEdge()
        setContent {
            var status by remember { mutableStateOf(Status.READY) }
            val url =
                "https://rapla.dhbw.de/rapla/calendar?key=SF8qHSuYFD3SStyfcj4vvmAhUMdwoDn7AYC1DTtyyBmhFJAv8m_hIYVHpm9Ul6nMjqX11N94dkWx78kCdoJxR44ru1kegzIBOMCCSJVRikkSTGNCV0YyThLBR30y9hOaGryjvwt1kpad5g93Dkdn0A&salt=-218630611"
            var raplaResult by remember { mutableStateOf<RaplaResult?>(null) }

            // changed: single root Column that fills the screen
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // top controls (no weight)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            status = Status.FETCHING
                            val stringRequest = StringRequest(Request.Method.GET, url, { html ->
                                status = Status.SUCCESS
                                val parsed = html2RaplaEvent(html)
                                // update the Compose state so UI recomposes
                                raplaResult = parsed
                                Log.d(
                                    "MainActivity",
                                    "Parsed Event Titles: ${parsed?.allEventTitles()}"
                                )
                            }, { error ->
                                status = Status.ERROR
                                Log.e("MainActivity", "Error fetching data: $error")
                            })
                            queue.add(stringRequest)
                        },
                        enabled = status != Status.FETCHING
                    ) {
                        Text(
                            when (status) {
                                Status.READY -> "Fetch Rapla Events"
                                Status.FETCHING -> "Fetching..."
                                Status.SUCCESS -> "Fetch completed."
                                Status.ERROR -> "Fetch failed. Retry?"
                            }
                        )
                    }
                }

                // content area takes remaining space so LazyColumn gets bounded height
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Upcoming Events",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )

                    Button(
                        onClick = {
                            getCalendarRequest()
                        },
                        enabled = status == Status.SUCCESS,
                        modifier = Modifier.padding(bottom = 15.dp)
                    ) { Text("Add Calender Events to Device") }

                    val events = raplaResult?.weeks?.flatMap { it.events }
                        ?: emptyList()

                    // LazyColumn now has bounded height provided by the parent Column.weight(1f)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(events) { event ->
                            EventListItem(
                                event.title,
                                event.course!!,
                                event.date,
                                event.startTime,
                                event.endTime,
                                event.room ?: "No location specified",
                            )
                        }
                    }
                }
            }
        }
    }

    fun html2RaplaEvent(html: String): RaplaResult? {
        val raplaParser = RaplaParser()
        return raplaParser.parse(html)
    }

    fun getCalendarRequest() {
        locationPermissionReqeust.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )

    }
}
