package com.jeba.bloomingtontransit.data.repository

object RepositoryProvider {
    val transitRepository: TransitRepository by lazy { TransitRepository() }
}