package com.jeba.bloomingtontransit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.jeba.bloomingtontransit.data.network.GtfsRealtimeParser
import com.jeba.bloomingtontransit.data.network.NetworkModule
import com.jeba.bloomingtontransit.ui.theme.BloomingtonTransitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jeba.bloomingtontransit.data.repository.RepositoryProvider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

import android.content.Context
import com.jeba.bloomingtontransit.data.db.StaticGtfsLoader
import com.jeba.bloomingtontransit.data.db.TransitDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bytes = NetworkModule.api.getVehiclePositions().bytes()
                val vehicles = GtfsRealtimeParser.parseVehiclePositions(bytes)

                Log.d("PARSER_TEST", "Found ${vehicles.size} vehicles")

                vehicles.take(3).forEach { v ->
                    Log.d(
                        "PARSER_TEST",
                        "ID=${v.vehicleId} route=${v.routeId} lat=${v.lat} lon=${v.lon}"
                    )
                }

            } catch (e: Exception) {
                Log.e("PARSER_TEST", "Error: ${e.message}")
            }
        }
        val db = TransitDatabase.getInstance(this)
        val loader = StaticGtfsLoader(this)

        lifecycleScope.launch(Dispatchers.IO) {
            loader.loadIfNeeded(db)
            // Confirm it worked:
            val stopCount = db.stopDao().count()
            val routeCount = db.routeDao().count()
            Log.d("DB_TEST", "DB ready: $stopCount stops, $routeCount routes")
        }
        val pollingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        RepositoryProvider.transitRepository.startPolling(pollingScope)

        enableEdgeToEdge()
        setContent {
            BloomingtonTransitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BloomingtonTransitTheme {
        Greeting("Android")
    }
}