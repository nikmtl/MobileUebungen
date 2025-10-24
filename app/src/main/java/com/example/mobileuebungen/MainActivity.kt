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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            var status by rememberSaveable { mutableStateOf(Status.READY)}
            val url =
                "https://rapla.dhbw.de/rapla/calendar?key=SF8qHSuYFD3SStyfcj4vvmAhUMdwoDn7AYC1DTtyyBmhFJAv8m_hIYVHpm9Ul6nMjqX11N94dkWx78kCdoJxR44ru1kegzIBOMCCSJVRikkSTGNCV0YyThLBR30y9hOaGryjvwt1kpad5g93Dkdn0A&salt=-218630611"

            Column(
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Button(
                    onClick = {
                        status = Status.FETCHING
                        val stringRequest = StringRequest(Request.Method.GET, url, {
                            status = Status.SUCCESS
                            Log.d("MainActivity", "Fetched HTML: $it")
                            val raplaEvent = html2RaplaEvent(it)
                            if (raplaEvent != null) {
                                val titles = raplaEvent.allEventTitles()
                                Log.d("MainActivity", "Parsed Event Titles: $titles")
                            } else {
                                Log.e("MainActivity", "Failed to parse Rapla events.")
                            }
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
                Button(
                    onClick = {
                        getCalendarRequest()
                    },
                    enabled = status == Status.SUCCESS
                ) { Text("Add Calender Events to Device") }
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

