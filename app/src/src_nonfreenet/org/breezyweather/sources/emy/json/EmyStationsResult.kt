package org.breezyweather.sources.emy.json

import kotlinx.serialization.Serializable

@Serializable
data class EmyStationsResult(
    val data: EmyStationsData? = null,
)
