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

package org.breezyweather.sources.weatherapi

import android.content.Context
import breezyweather.domain.location.model.Location
import breezyweather.domain.location.model.LocationAddressInfo
import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.model.AirQuality
import breezyweather.domain.weather.model.Alert
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.PrecipitationProbability
import breezyweather.domain.weather.model.UV
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.reference.AlertSeverity
import breezyweather.domain.weather.reference.WeatherCode
import breezyweather.domain.weather.wrappers.AirQualityWrapper
import breezyweather.domain.weather.wrappers.CurrentWrapper
import breezyweather.domain.weather.wrappers.DailyWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.TemperatureWrapper
import breezyweather.domain.weather.wrappers.WeatherWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.BuildConfig
import org.breezyweather.R
import org.breezyweather.common.exceptions.InvalidLocationException
import org.breezyweather.common.extensions.code
import org.breezyweather.common.extensions.currentLocale
import org.breezyweather.common.preference.EditTextPreference
import org.breezyweather.common.preference.Preference
import org.breezyweather.common.utils.ISO8601Utils
import org.breezyweather.domain.settings.SourceConfigStore
import org.breezyweather.sources.weatherapi.json.WeatherApiAlert
import org.breezyweather.sources.weatherapi.json.WeatherApiCurrent
import org.breezyweather.sources.weatherapi.json.WeatherApiForecast
import org.breezyweather.unit.distance.Distance.Companion.kilometers
import org.breezyweather.unit.pollutant.PollutantConcentration.Companion.microgramsPerCubicMeter
import org.breezyweather.unit.precipitation.Precipitation.Companion.centimeters
import org.breezyweather.unit.precipitation.Precipitation.Companion.millimeters
import org.breezyweather.unit.pressure.Pressure.Companion.hectopascals
import org.breezyweather.unit.ratio.Ratio.Companion.percent
import org.breezyweather.unit.speed.Speed.Companion.kilometersPerHour
import org.breezyweather.unit.temperature.Temperature.Companion.celsius
import retrofit2.Retrofit
import java.util.Date
import java.util.Objects
import javax.inject.Inject
import javax.inject.Named

class WeatherApiService @Inject constructor(
    @ApplicationContext context: Context,
    @Named("JsonClient") client: Retrofit.Builder,
) : WeatherApiServiceStub(context) {

    override val privacyPolicyUrl = "https://www.weatherapi.com/privacy.aspx"

    override val attributionLinks = mapOf(
        "WeatherAPI.com" to "https://www.weatherapi.com/"
    )

    private val mApi by lazy {
        client
            .baseUrl(WEATHER_API_BASE_URL)
            .build()
            .create(WeatherApiApi::class.java)
    }

    override fun requestWeather(
        context: Context,
        location: Location,
        requestedFeatures: List<SourceFeature>,
    ): Observable<WeatherWrapper> {
        val apiKey = getApiKeyOrDefault()

        return mApi.getWeather(
            key = apiKey,
            q = "${location.latitude},${location.longitude}",
            lang = getRequestLanguage(context)
        ).map {
            WeatherWrapper(
                current = getCurrent(it.current),
                dailyForecast = getDailyForecast(it.forecast),
                hourlyForecast = getHourlyForecast(it.forecast),
                airQuality = getAirQuality(it.current, it.forecast),
                alertList = getAlertList(it.alerts?.alerts)
            )
        }.onErrorResumeNext {
            Observable.just(
                WeatherWrapper(
                    failedFeatures = requestedFeatures.associateWith { feature -> it }
                )
            )
        }
    }

    private fun getRequestLanguage(
        context: Context,
    ): String {
        return when {
            TRADITIONAL_CHINESE.any { it.equals(context.currentLocale.code, ignoreCase = true) } -> "zh_tw"
            WEATHER_API_LANGUAGES.any {
                it.equals(context.currentLocale.language, ignoreCase = true)
            } -> context.currentLocale.language.lowercase()
            else -> "en"
        }
    }

    private fun getCurrent(
        current: WeatherApiCurrent?,
    ): CurrentWrapper {
        return CurrentWrapper(
            weatherText = current?.condition?.text,
            weatherCode = getWeatherCode(current?.condition?.code),
            temperature = TemperatureWrapper(
                temperature = current?.temp?.celsius,
                feelsLike = current?.feelsLike?.celsius
            ),
            wind = Wind(
                degree = current?.windDegree,
                speed = current?.wind?.kilometersPerHour,
                gusts = current?.gust?.kilometersPerHour
            ),
            uV = UV(
                index = current?.uv
            ),
            relativeHumidity = current?.humidity?.percent,
            dewPoint = current?.dewPoint?.celsius,
            pressure = current?.pressure?.hectopascals,
            cloudCover = current?.cloud?.percent,
            visibility = current?.vis?.kilometers
        )
    }

    private fun getDailyForecast(
        forecast: WeatherApiForecast?,
    ): List<DailyWrapper>? {
        return forecast?.forecastDays?.filter {
            it.hours?.size!! > 0
        }?.map {
            DailyWrapper(
                date = Date(it.hours?.first()!!.time.times(1000))
            )
        }
    }

    private fun getHourlyForecast(
        forecast: WeatherApiForecast?,
    ): List<HourlyWrapper>? {
        val hourlyList = mutableListOf<HourlyWrapper>()
        forecast?.forecastDays?.forEach { day ->
            day.hours?.forEach {
                hourlyList.add(
                    HourlyWrapper(
                        date = Date(it.time.times(1000)),
                        weatherText = it.condition?.text,
                        weatherCode = getWeatherCode(it.condition?.code),
                        temperature = TemperatureWrapper(
                            temperature = it.temp?.celsius,
                            feelsLike = it.feelsLike?.celsius
                        ),
                        precipitation = Precipitation(
                            rain = it.precip?.millimeters,
                            snow = it.snow?.centimeters
                        ),
                        precipitationProbability = PrecipitationProbability(
                            rain = it.chanceOfRain?.percent,
                            snow = it.chanceOfSnow?.percent
                        ),
                        wind = Wind(
                            degree = it.windDegree,
                            speed = it.wind?.kilometersPerHour,
                            gusts = it.gust?.kilometersPerHour
                        ),
                        uV = UV(
                            index = it.uv
                        ),
                        relativeHumidity = it.humidity?.percent,
                        dewPoint = it.dewPoint?.celsius,
                        pressure = it.pressure?.hectopascals,
                        cloudCover = it.cloud?.percent,
                        visibility = it.vis?.kilometers
                    )
                )
            }
        }
        return hourlyList
    }

    private fun getAirQuality(
        current: WeatherApiCurrent?,
        forecast: WeatherApiForecast?,
    ): AirQualityWrapper {
        val hourlyMap = mutableMapOf<Date, AirQuality>()
        forecast?.forecastDays?.forEach { day ->
            day.hours?.forEach {
                hourlyMap[Date(it.time.times(1000))] = AirQuality(
                    pM25 = it.airQuality?.pm25?.microgramsPerCubicMeter,
                    pM10 = it.airQuality?.pm10?.microgramsPerCubicMeter,
                    sO2 = it.airQuality?.so2?.microgramsPerCubicMeter,
                    nO2 = it.airQuality?.no2?.microgramsPerCubicMeter,
                    o3 = it.airQuality?.o3?.microgramsPerCubicMeter,
                    cO = it.airQuality?.co?.microgramsPerCubicMeter
                )
            }
        }
        return AirQualityWrapper(
            current = AirQuality(
                pM25 = current?.airQuality?.pm25?.microgramsPerCubicMeter,
                pM10 = current?.airQuality?.pm10?.microgramsPerCubicMeter,
                sO2 = current?.airQuality?.so2?.microgramsPerCubicMeter,
                nO2 = current?.airQuality?.no2?.microgramsPerCubicMeter,
                o3 = current?.airQuality?.o3?.microgramsPerCubicMeter,
                cO = current?.airQuality?.co?.microgramsPerCubicMeter
            )
        )
    }

    private fun getAlertList(
        alerts: List<WeatherApiAlert>?,
    ): List<Alert>? {
        val regex = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[\+\-]\d{2}:\d{2}$""")
        return alerts?.filter {
            it.category.equals("Met", ignoreCase = true) &&
                !it.msgtype.equals("Cancel", ignoreCase = true) &&
                !it.urgency.equals("Past", ignoreCase = true)
        }?.map {
            val severity = with(it.severity?.trim()) {
                when {
                    equals("Extreme", ignoreCase = true) -> AlertSeverity.EXTREME
                    equals("Severe", ignoreCase = true) -> AlertSeverity.SEVERE
                    equals("Moderate", ignoreCase = true) -> AlertSeverity.MODERATE
                    equals("Minor", ignoreCase = true) -> AlertSeverity.MINOR
                    else -> AlertSeverity.UNKNOWN
                }
            }
            val title = it.event?.trim() ?: it.headline?.trim()
            val startDate = if (it.effective?.trim()?.matches(regex) ?: false) {
                ISO8601Utils.parse(it.effective.trim())
            } else {
                null
            }
            val endDate = if (it.expires?.trim()?.matches(regex) ?: false) {
                ISO8601Utils.parse(it.expires.trim())
            } else {
                null
            }

            Alert(
                alertId = Objects.hash(title, severity, startDate).toString(),
                startDate = startDate,
                endDate = endDate,
                headline = title,
                description = it.desc?.trim(),
                instruction = it.instruction?.trim(),
                severity = severity,
                color = Alert.colorFromSeverity(severity)
            )
        } ?: emptyList()
    }

    private fun getWeatherCode(
        code: Int?,
    ): WeatherCode? {
        return when (code) {
            1000 -> WeatherCode.CLEAR
            1003 -> WeatherCode.PARTLY_CLOUDY
            1006, 1009 -> WeatherCode.CLOUDY
            1030, 1135, 1147 -> WeatherCode.FOG
            1063, 1066, 1150, 1153, 1180, 1183, 1186, 1189, 1192, 1195,
            1240, 1243, 1246,
            -> WeatherCode.RAIN
            1069, 1072, 1168, 1171, 1198, 1201, 1204, 1207, 1249, 1252 -> WeatherCode.SLEET
            1087 -> WeatherCode.THUNDER
            1114, 1117, 1210, 1213, 1216, 1219, 1222, 1225, 1237, 1255,
            1258, 1261, 1264, 1279, 1282,
            -> WeatherCode.SNOW
            1273, 1276 -> WeatherCode.THUNDERSTORM
            else -> null
        }
    }

    override fun requestNearestLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): Observable<List<LocationAddressInfo>> {
        val apiKey = getApiKeyOrDefault()
        return mApi.getLocation(
            key = apiKey,
            q = "$latitude,$longitude"
        ).map {
            if (it.location?.name == null) {
                throw InvalidLocationException()
            }
            listOf(
                LocationAddressInfo(
                    country = it.location.country,
                    countryCode = "",
                    admin1 = it.location.region,
                    city = it.location.name,
                    timeZoneId = it.location.tzId
                )
            )
        }
    }

    // CONFIG
    private val config = SourceConfigStore(context, id)
    private var apikey: String
        set(value) {
            config.edit().putString("apikey", value).apply()
        }
        get() = config.getString("apikey", null) ?: ""

    private fun getApiKeyOrDefault(): String {
        return apikey.ifEmpty { BuildConfig.WEATHER_API_KEY }
    }

    override val isConfigured
        get() = getApiKeyOrDefault().isNotEmpty()

    override val isRestricted
        get() = apikey.isEmpty()

    override fun getPreferences(context: Context): List<Preference> {
        return listOf(
            EditTextPreference(
                titleId = R.string.settings_weather_source_weatherapi_api_key,
                summary = { c, content ->
                    content.ifEmpty {
                        c.getString(R.string.settings_source_default_value)
                    }
                },
                content = apikey,
                onValueChanged = {
                    apikey = it
                }
            )
        )
    }

    companion object {
        private const val WEATHER_API_BASE_URL = "https://api.weatherapi.com/"
        private val TRADITIONAL_CHINESE = arrayOf("zh-TW", "zh-HK", "zh-MO")
        private val WEATHER_API_LANGUAGES = arrayOf(
            "ar", "bn", "bg", "zh", "cs", "da", "nl", "fi", "fr", "de", "el", "hi",
            "hu", "it", "ja", "jv", "ko", "mr", "pl", "pt", "pa", "ro", "ru", "sr",
            "si", "sk", "es", "sv", "ta", "te", "tr", "uk", "ur", "vi", "zu"
        )
    }
}
