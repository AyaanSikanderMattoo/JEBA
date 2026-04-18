package com.jeba.bloomingtontransit.data.db

import android.content.Context
import android.util.Log
import com.jeba.bloomingtontransit.data.db.entity.RouteEntity
import com.jeba.bloomingtontransit.data.db.entity.ShapePointEntity
import com.jeba.bloomingtontransit.data.db.entity.StopEntity
import com.jeba.bloomingtontransit.data.db.entity.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class StaticGtfsLoader(private val context: Context) {

    private val TAG = "GTFS_LOADER"
    private val PREFS_NAME = "gtfs_prefs"
    private val KEY_LOADED = "static_loaded_v2"   // bumped so it reloads with trips table

    suspend fun loadIfNeeded(db: TransitDatabase) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LOADED, false)) {
            Log.d(TAG, "Static GTFS already loaded, skipping")
            return@withContext
        }

        Log.d(TAG, "Loading static GTFS from assets...")
        try {
            val zipStream = ZipInputStream(context.assets.open("google_transit.zip"))
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (!entry.name.startsWith("__MACOSX")) {
                    val reader = BufferedReader(InputStreamReader(zipStream, Charsets.UTF_8))
                    when (entry.name) {
                        "stops.txt"  -> parseAndInsertStops(reader, db)
                        "routes.txt" -> parseAndInsertRoutes(reader, db)
                        "shapes.txt" -> parseAndInsertShapes(reader, db)
                        "trips.txt"  -> parseAndInsertTrips(reader, db)
                        else -> {}
                    }
                }
                zipStream.closeEntry()
                entry = zipStream.nextEntry
            }
            zipStream.close()
            prefs.edit().putBoolean(KEY_LOADED, true).apply()
            Log.d(TAG, "Static GTFS load complete!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load static GTFS: ${e.message}")
        }
    }

    private suspend fun parseAndInsertStops(reader: BufferedReader, db: TransitDatabase) {
        val stops = mutableListOf<StopEntity>()
        val cols = reader.readLine()?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: return
        val idIdx = cols.indexOf("stop_id"); val nameIdx = cols.indexOf("stop_name")
        val latIdx = cols.indexOf("stop_lat"); val lonIdx = cols.indexOf("stop_lon")
        if (listOf(idIdx, nameIdx, latIdx, lonIdx).any { it == -1 }) return
        reader.lineSequence().forEach { line ->
            val p = parseCsvLine(line)
            try { stops.add(StopEntity(p[idIdx], p[nameIdx], p[latIdx].toDouble(), p[lonIdx].toDouble())) } catch (_: Exception) {}
        }
        db.stopDao().insertAll(stops)
        Log.d(TAG, "Inserted ${stops.size} stops")
    }

    private suspend fun parseAndInsertRoutes(reader: BufferedReader, db: TransitDatabase) {
        val routes = mutableListOf<RouteEntity>()
        val cols = reader.readLine()?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: return
        val idIdx = cols.indexOf("route_id"); val shortIdx = cols.indexOf("route_short_name")
        val longIdx = cols.indexOf("route_long_name"); val colorIdx = cols.indexOf("route_color")
        if (listOf(idIdx, shortIdx, longIdx).any { it == -1 }) return
        reader.lineSequence().forEach { line ->
            val p = parseCsvLine(line)
            try { routes.add(RouteEntity(p[idIdx], p[shortIdx], p[longIdx], if (colorIdx != -1 && colorIdx < p.size) p[colorIdx] else "0070C0")) } catch (_: Exception) {}
        }
        db.routeDao().insertAll(routes)
        Log.d(TAG, "Inserted ${routes.size} routes")
    }

    private suspend fun parseAndInsertShapes(reader: BufferedReader, db: TransitDatabase) {
        val points = mutableListOf<ShapePointEntity>()
        val cols = reader.readLine()?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: return
        val idIdx = cols.indexOf("shape_id"); val latIdx = cols.indexOf("shape_pt_lat")
        val lonIdx = cols.indexOf("shape_pt_lon"); val seqIdx = cols.indexOf("shape_pt_sequence")
        if (listOf(idIdx, latIdx, lonIdx, seqIdx).any { it == -1 }) return
        reader.lineSequence().forEach { line ->
            val p = parseCsvLine(line)
            try { points.add(ShapePointEntity(shapeId = p[idIdx], lat = p[latIdx].toDouble(), lon = p[lonIdx].toDouble(), sequence = p[seqIdx].toInt())) } catch (_: Exception) {}
            if (points.size >= 500) { db.shapePointDao().insertAll(points.toList()); points.clear() }
        }
        if (points.isNotEmpty()) db.shapePointDao().insertAll(points)
        Log.d(TAG, "Inserted shape points")
    }

    private suspend fun parseAndInsertTrips(reader: BufferedReader, db: TransitDatabase) {
        val trips = mutableListOf<TripEntity>()
        val cols = reader.readLine()?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: return
        val routeIdx = cols.indexOf("route_id"); val tripIdx = cols.indexOf("trip_id")
        val headsignIdx = cols.indexOf("trip_headsign")
        if (listOf(routeIdx, tripIdx).any { it == -1 }) return
        reader.lineSequence().forEach { line ->
            val p = parseCsvLine(line)
            try {
                trips.add(TripEntity(
                    tripId = p[tripIdx],
                    routeId = p[routeIdx],
                    tripHeadsign = if (headsignIdx != -1 && headsignIdx < p.size) p[headsignIdx] else ""
                ))
            } catch (_: Exception) {}
            if (trips.size >= 500) { db.tripDao().insertAll(trips.toList()); trips.clear() }
        }
        if (trips.isNotEmpty()) db.tripDao().insertAll(trips)
        Log.d(TAG, "Inserted ${db.tripDao().count()} trips")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> { result.add(current.toString().trim()); current = StringBuilder() }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}