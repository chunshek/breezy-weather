/**
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.sources.emy

import android.content.Context
import android.util.Log
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceContinent
import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.wrappers.WeatherWrapper
import com.google.maps.android.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.common.exceptions.InvalidLocationException
import org.breezyweather.common.extensions.code
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.source.HttpSource
import org.breezyweather.common.source.LocationParametersSource
import org.breezyweather.common.source.WeatherSource
import org.breezyweather.common.source.WeatherSource.Companion.PRIORITY_HIGHEST
import org.breezyweather.common.source.WeatherSource.Companion.PRIORITY_NONE
import org.breezyweather.sources.emy.json.EmyStation
import org.breezyweather.sources.emy.json.EmyStationsResult
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

class EmyService @Inject constructor(
    @ApplicationContext context: Context,
    @Named("JsonClient") client: Retrofit.Builder,
) : HttpSource(), WeatherSource, LocationParametersSource {

    override val id = "emy"
    override val name by lazy {
        with(context.currentLocale.code) {
            when {
                startsWith("el") -> "Εθνική Μετεωρολογική Υπηρεσία"
                else -> "Hellenic National Meteorological Service"
            }
        }
    }
    override val continent = SourceContinent.EUROPE
    override val privacyPolicyUrl by lazy {
        with(context.currentLocale.code) {
            when {
                startsWith("el") -> "https://www.emy.gr/privacy-policy"
                else -> "https://www.emy.gr/en/privacy-policy"
            }
        }
    }

    private val mApi by lazy {
        client
            .baseUrl(EMY_API_BASE_URL)
            .build()
            .create(EmyApi::class.java)
    }

    private val weatherAttribution = name

    override val supportedFeatures = mapOf(
        SourceFeature.FORECAST to weatherAttribution,
        SourceFeature.CURRENT to weatherAttribution,
        SourceFeature.ALERT to weatherAttribution,
        SourceFeature.NORMALS to weatherAttribution
    )

    override fun isFeatureSupportedForLocation(
        location: Location,
        feature: SourceFeature
    ): Boolean {
        return location.countryCode.equals("gr", ignoreCase = true)
    }

    override fun getFeaturePriorityForLocation(
        location: Location,
        feature: SourceFeature
    ): Int {
        return when {
            isFeatureSupportedForLocation(location, feature) -> PRIORITY_HIGHEST
            else -> PRIORITY_NONE
        }
    }

    override fun requestWeather(
        context: Context,
        location: Location,
        requestedFeatures: List<SourceFeature>
    ): Observable<WeatherWrapper> {
        return Observable.just(WeatherWrapper())
    }

//    override fun requestNearestLocation(
//        context: Context,
//        latitude: Double,
//        longitude: Double
//    ): Observable<List<LocationAddressInfo>> {
//        TODO("Not yet implemented")
//    }

    override fun needsLocationParametersRefresh(
        location: Location,
        coordinatesChanged: Boolean,
        features: List<SourceFeature>
    ): Boolean {
        if (coordinatesChanged) return true

        val forecastId = location.parameters.getOrElse(id) { null }?.getOrElse("forecastId") { null }
        val currentId = location.parameters.getOrElse(id) { null }?.getOrElse("currentId") { null }
//        val alertId = location.parameters.getOrElse(id) { null }?.getOrElse("alertId") { null }
        val normalsId = location.parameters.getOrElse(id) { null }?.getOrElse("normalsId") { null }

        return forecastId.isNullOrEmpty() ||
            currentId.isNullOrEmpty() ||
//            alertId.isNullOrEmpty() ||
            normalsId.isNullOrEmpty()
    }

    override fun requestLocationParameters(
        context: Context,
        location: Location
    ): Observable<Map<String, String>> {
        val forecastStations = mApi.getForecastStations()
        val currentStations = mApi.getCurrentStations()
        val normalsStations = mApi.getNormalsStations()
        return Observable.zip(forecastStations, currentStations, normalsStations) {
                forecastStationsResult: EmyStationsResult,
                currentStationsResult: EmyStationsResult,
                normalsStationsResult: EmyStationsResult,
            ->
            convertLocation(
                location,
                forecastStationsResult.data?.stations,
                currentStationsResult.data?.stations,
                normalsStationsResult.data?.stations
            )
        }
    }

    private fun convertLocation(
        location: Location,
        forecastStations: List<EmyStation>?,
        currentStations: List<EmyStation>?,
        normalsStations: List<EmyStation>?
    ): Map<String, String> {
        val forecastId = LatLng(location.latitude, location.longitude).getNearestLocation(
            forecastStations?.associate { it.id.toString() to LatLng(it.latitude, it.longitude) }, 50000.0
        )
        val currentId = LatLng(location.latitude, location.longitude).getNearestLocation(
            currentStations?.associate { it.id.toString() to LatLng(it.latitude, it.longitude) }, 50000.0
        )
        val normalsId = LatLng(location.latitude, location.longitude).getNearestLocation(
            normalsStations?.associate { it.id.toString() to LatLng(it.latitude, it.longitude) }, 50000.0
        )
        if (forecastId.isNullOrEmpty() || currentId.isNullOrEmpty() || normalsId.isNullOrEmpty()) {
            throw InvalidLocationException()
        }
        Log.d("emy-forecastId", forecastId)
        Log.d("emy-forecastId", forecastId)
        Log.d("emy-forecastId", forecastId)
        return mapOf(
            "forecastId" to forecastId,
            "currentId" to currentId,
            "normalsId" to normalsId
        )
    }

//    // Only supports its own country
//    override val knownAmbiguousCountryCodes: Array<String>? = null

    override val testingLocations: List<Location> = emptyList()

    companion object {
        private const val EMY_API_BASE_URL = "https://api.emy.gr/"
        private const val EMY_WWW_BASE_URL = "https://www.emy.gr/"
    }
}

