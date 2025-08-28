package org.breezyweather.sources.emy.json

import kotlinx.serialization.Serializable

@Serializable
data class EmyStation(
    val id: Int,
    val name: String?,
    val localName: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
)
