package com.jeba.bloomingtontransit.data.db

import android.content.Context
import android.util.Log
import com.jeba.bloomingtontransit.data.db.entity.RouteEntity
import com.jeba.bloomingtontransit.data.db.entity.ShapePointEntity
import com.jeba.bloomingtontransit.data.db.entity.StopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class StaticGtfsLoader(private val context: Context) {

    private val TAG = "GTFS_LOADER"
    private val PREFS_NAME = "gtfs_prefs"
    private val KEY_LOADED = "static_loaded"

    suspend fun loadIfNeeded(db: TransitDatabase) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyLoaded = prefs.getBoolean(KEY_LOADED, false)

        if (alreadyLoaded) {
            Log.d(TAG, "Static GTFS already loaded, skipping")
            return@withContext
        }

        Log.d(TAG, "Loading static GTFS from assets...")

        try {
            val zipStream = ZipInputStream(context.assets.open("google_transit.zip"))
            var entry = zipStream.nextEntry

            while (entry != null) {
                Log.d(TAG, "Processing ZIP entry: ${entry.name}")
                val reader = BufferedReader(InputStreamReader(zipStream, Charsets.UTF_8))

                when (entry.name) {
                    "stops.txt" -> parseAndInsertStops(reader, db)
                    "routes.txt" -> parseAndInsertRoutes(reader, db)
                    "shapes.txt" -> parseAndInsertShapes(reader, db)
                    else -> Log.d(TAG, "Skipping: ${entry.name}")
                }

                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }

            zipStream.close()

            prefs.edit().putBoolean(KEY_LOADED, true).apply()
            Log.d(TAG, "Static GTFS load complete!")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load static GTFS: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun parseAndInsertStops(reader: BufferedReader, db: TransitDatabase) {
        val stops = mutableListOf<StopEntity>()
        val header = reader.readLine() ?: return
        val cols = header.split(",").map { it.trim().removeSurrounding("\"") }

        val idIdx = cols.indexOf("stop_id")
        val nameIdx = cols.indexOf("stop_name")
        val latIdx = cols.indexOf("stop_lat")
        val lonIdx = cols.indexOf("stop_lon")

        if (listOf(idIdx, nameIdx, latIdx, lonIdx).any { it == -1 }) {
            Log.e(TAG, "stops.txt missing required columns. Found: $cols")
            return
        }

        reader.lineSequence().forEach { line ->
            val parts = parseCsvLine(line)
            if (parts.size > maxOf(idIdx, nameIdx, latIdx, lonIdx)) {
                try {
                    stops.add(
                        StopEntity(
                            stopId = parts[idIdx],
                            stopName = parts[nameIdx],
                            lat = parts[latIdx].toDouble(),
                            lon = parts[lonIdx].toDouble()
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed rows silently
                }
            }
        }

        db.stopDao().insertAll(stops)
        Log.d(TAG, "Inserted ${stops.size} stops")
    }

    private suspend fun parseAndInsertRoutes(reader: BufferedReader, db: TransitDatabase) {
        val routes = mutableListOf<RouteEntity>()
        val header = reader.readLine() ?: return
        val cols = header.split(",").map { it.trim().removeSurrounding("\"") }

        val idIdx = cols.indexOf("route_id")
        val shortIdx = cols.indexOf("route_short_name")
        val longIdx = cols.indexOf("route_long_name")
        val colorIdx = cols.indexOf("route_color")

        if (listOf(idIdx, shortIdx, longIdx).any { it == -1 }) {
            Log.e(TAG, "routes.txt missing required columns. Found: $cols")
            return
        }

        reader.lineSequence().forEach { line ->
            val parts = parseCsvLine(line)
            if (parts.size > maxOf(idIdx, shortIdx, longIdx)) {
                try {
                    routes.add(
                        RouteEntity(
                            routeId = parts[idIdx],
                            shortName = parts[shortIdx],
                            longName = parts[longIdx],
                            color = if (colorIdx != -1 && colorIdx < parts.size) parts[colorIdx] else "0070C0"
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed rows silently
                }
            }
        }

        db.routeDao().insertAll(routes)
        Log.d(TAG, "Inserted ${routes.size} routes")
    }

    private suspend fun parseAndInsertShapes(reader: BufferedReader, db: TransitDatabase) {
        val points = mutableListOf<ShapePointEntity>()
        val header = reader.readLine() ?: return
        val cols = header.split(",").map { it.trim().removeSurrounding("\"") }

        val idIdx = cols.indexOf("shape_id")
        val latIdx = cols.indexOf("shape_pt_lat")
        val lonIdx = cols.indexOf("shape_pt_lon")
        val seqIdx = cols.indexOf("shape_pt_sequence")

        if (listOf(idIdx, latIdx, lonIdx, seqIdx).any { it == -1 }) {
            Log.e(TAG, "shapes.txt missing required columns. Found: $cols")
            return
        }

        reader.lineSequence().forEach { line ->
            val parts = parseCsvLine(line)
            if (parts.size > maxOf(idIdx, latIdx, lonIdx, seqIdx)) {
                try {
                    points.add(
                        ShapePointEntity(
                            shapeId = parts[idIdx],
                            lat = parts[latIdx].toDouble(),
                            lon = parts[lonIdx].toDouble(),
                            sequence = parts[seqIdx].toInt()
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed rows silently
                }
            }

            // Insert in batches of 500 to avoid memory issues with large files
            if (points.size >= 500) {
                db.shapePointDao().insertAll(points.toList())
                points.clear()
            }
        }

        if (points.isNotEmpty()) {
            db.shapePointDao().insertAll(points)
        }

        Log.d(TAG, "Inserted shape points")
    }

    // Handles quoted CSV fields correctly
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}