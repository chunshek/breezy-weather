package org.breezyweather.sources.emy.json

import kotlinx.serialization.Serializable

@Serializable
data class EmyStationsData(
    val stations: List<EmyStation>?
)
