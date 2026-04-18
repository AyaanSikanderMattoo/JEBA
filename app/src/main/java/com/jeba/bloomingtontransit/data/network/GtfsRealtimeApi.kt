package com.jeba.bloomingtontransit.data.network

import okhttp3.ResponseBody
import retrofit2.http.GET

interface GtfsRealtimeApi {

    @GET("position_updates.pb")
    suspend fun getVehiclePositions(): ResponseBody

    @GET("trip_updates.pb")
    suspend fun getTripUpdates(): ResponseBody
}