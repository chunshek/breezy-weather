/*
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

package org.breezyweather.sources.weatherapi.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherApiForecastHour(
    @SerialName("time_epoch") val time: Long,
    @SerialName("temp_c") val temp: Double?,
    val condition: WeatherApiCondition?,
    @SerialName("wind_kph") val wind: Double?,
    @SerialName("wind_degree") val windDegree: Double?,
    @SerialName("pressure_mb") val pressure: Double?,
    @SerialName("precip_mm") val precip: Double?,
    @SerialName("snow_cm") val snow: Double?,
    val humidity: Double?,
    val cloud: Double?,
    @SerialName("feelslike_c") val feelsLike: Double?,
    @SerialName("dewpoint_c") val dewPoint: Double?,
    @SerialName("chance_of_rain") val chanceOfRain: Double?,
    @SerialName("chance_of_snow") val chanceOfSnow: Double?,
    @SerialName("vis_km") val vis: Double?,
    @SerialName("gust_kph") val gust: Double?,
    val uv: Double?,
    @SerialName("air_quality") val airQuality: WeatherApiAirQuality?,
)
