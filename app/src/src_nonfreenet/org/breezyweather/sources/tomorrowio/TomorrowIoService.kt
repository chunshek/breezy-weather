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

package org.breezyweather.sources.tomorrowio

import android.content.Context
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceFeature
import breezyweather.domain.weather.model.Minutely
import breezyweather.domain.weather.model.Precipitation
import breezyweather.domain.weather.model.PrecipitationProbability
import breezyweather.domain.weather.model.UV
import breezyweather.domain.weather.model.Wind
import breezyweather.domain.weather.reference.WeatherCode
import breezyweather.domain.weather.wrappers.DailyWrapper
import breezyweather.domain.weather.wrappers.HourlyWrapper
import breezyweather.domain.weather.wrappers.TemperatureWrapper
import breezyweather.domain.weather.wrappers.WeatherWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Observable
import org.breezyweather.BuildConfig
import org.breezyweather.R
import org.breezyweather.common.preference.EditTextPreference
import org.breezyweather.common.preference.Preference
import org.breezyweather.domain.settings.SourceConfigStore
import org.breezyweather.sources.tomorrowio.json.TomorrowIoForecastResult
import org.breezyweather.sources.tomorrowio.json.TomorrowIoWeather
import org.breezyweather.unit.computing.computeTotalPrecipitation
import org.breezyweather.unit.distance.Distance.Companion.kilometers
import org.breezyweather.unit.precipitation.Precipitation.Companion.millimeters
import org.breezyweather.unit.pressure.Pressure.Companion.hectopascals
import org.breezyweather.unit.ratio.Ratio.Companion.percent
import org.breezyweather.unit.speed.Speed.Companion.metersPerSecond
import org.breezyweather.unit.temperature.Temperature.Companion.celsius
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Named

class TomorrowIoService @Inject constructor(
    @ApplicationContext context: Context,
    @Named("JsonClient") client: Retrofit.Builder,
) : TomorrowIoServiceStub(context) {

    override val privacyPolicyUrl = "https://www.tomorrow.io/legal/product-privacy-policy/"

    private val mApi by lazy {
        client
            .baseUrl(TOMORROW_IO_BASE_URL)
            .build()
            .create(TomorrowIoApi::class.java)
    }

    override fun requestWeather(
        context: Context,
        location: Location,
        requestedFeatures: List<SourceFeature>
    ): Observable<WeatherWrapper> {
        val apikey = getApiKeyOrDefault()
        val failedFeatures = mutableMapOf<SourceFeature, Throwable>()
        val forecast = mApi.getForecast(
            apikey = apikey,
            location = "${location.latitude},${location.longitude}"
        ).onErrorResumeNext {
            failedFeatures[SourceFeature.FORECAST] = it
            Observable.just(TomorrowIoForecastResult())
        }
        return forecast.map {
            WeatherWrapper(
                dailyForecast = getDailyForecast(it.timelines?.daily),
                hourlyForecast = getHourlyForecast(context, it.timelines?.hourly),
                minutelyForecast = getMinutelyForecast(it.timelines?.minutely)
            )
        }
    }

    private fun getDailyForecast(
        daily: List<TomorrowIoWeather>?
    ): List<DailyWrapper>? {
        return daily?.map {
            DailyWrapper(
                date = it.time
            )
        }
    }

    private fun getHourlyForecast(
        context: Context,
        hourly: List<TomorrowIoWeather>?
    ): List<HourlyWrapper>? {
        return hourly?.map {
            HourlyWrapper(
                date = it.time,
                weatherText = getWeatherText(context, it.values?.weatherCode),
                weatherCode = getWeatherCode(it.values?.weatherCode),
                temperature = TemperatureWrapper(
                    temperature = it.values?.temperature?.celsius,
                    feelsLike = it.values?.temperatureApparent?.celsius
                ),
                precipitation = Precipitation(
                    rain = it.values?.rainAccumulation?.millimeters,
                    snow = it.values?.snowAccumulation?.millimeters,
                    ice = it.values?.iceAccumulation?.millimeters,
                ),
                precipitationProbability = PrecipitationProbability(
                    total = it.values?.precipitationProbability?.percent,
                    thunderstorm = it.values?.thunderstormProbability?.percent
                ),
                wind = Wind(
                    degree = it.values?.windDirection,
                    speed = it.values?.windSpeed?.metersPerSecond,
                    gusts = it.values?.windGust?.metersPerSecond
                ),
                uV = UV(
                    index = it.values?.uvIndex
                ),
                relativeHumidity = it.values?.humidity?.percent,
                dewPoint = it.values?.dewPoint?.celsius,
                pressure = it.values?.pressureSeaLevel?.hectopascals,
                cloudCover = it.values?.cloudCover?.percent,
                visibility = it.values?.visibility?.kilometers
            )
        }
    }

    private fun getMinutelyForecast(
        minutely: List<TomorrowIoWeather>?
    ): List<Minutely>? {
        return minutely?.map {
            Minutely(
                date = it.time,
                minuteInterval = 1,
                precipitationIntensity = computeTotalPrecipitation(
                    temperature = it.values?.temperature?.celsius,
                    rain = ((it.values?.rainIntensity ?: 0.0) + (it.values?.freezingRainIntensity ?: 0.0)).millimeters,
                    snow = it.values?.snowIntensity?.millimeters,
                    ice = it.values?.sleetIntensity?.millimeters
                )
            )
        }
    }

    private fun getWeatherText(
        context: Context,
        code: Int?
    ): String? {
        return when (code) {
            1000 -> context.getString(R.string.common_weather_text_clear_sky)
            1100 -> context.getString(R.string.common_weather_text_mostly_clear)
            1101 -> context.getString(R.string.common_weather_text_partly_cloudy)
            1102 -> context.getString(R.string.common_weather_text_mostly_cloudy)
            1001 -> context.getString(R.string.common_weather_text_cloudy)
            2000 -> context.getString(R.string.common_weather_text_fog)
            2100 -> context.getString(R.string.common_weather_text_fog) // Light fog
            4000 -> context.getString(R.string.common_weather_text_drizzle)
            4001 -> context.getString(R.string.common_weather_text_rain)
            4200 -> context.getString(R.string.common_weather_text_rain_light)
            4201 -> context.getString(R.string.common_weather_text_rain_heavy)
            5000 -> context.getString(R.string.common_weather_text_snow)
            5001 -> "Flurries"
            5100 -> context.getString(R.string.common_weather_text_snow_light)
            5101 -> context.getString(R.string.common_weather_text_snow_heavy)
            6000 -> context.getString(R.string.common_weather_text_drizzle_freezing)
            6001 -> context.getString(R.string.common_weather_text_rain_freezing)
            6200 -> context.getString(R.string.common_weather_text_rain_freezing_light)
            6201 -> context.getString(R.string.common_weather_text_rain_freezing_heavy)
            7000 -> "Ice pellets"
            7101 -> "Heavy ice pellets"
            7102 -> "Light ice pellets"
            8000 -> context.getString(R.string.weather_kind_thunderstorm)
            else -> null
        }
    }

    private fun getWeatherCode(
        code: Int?
    ): WeatherCode? {
        return when (code) {
            1000, 1100 -> WeatherCode.CLEAR
            1101 -> WeatherCode.PARTLY_CLOUDY
            1102, 1001 -> WeatherCode.CLOUDY
            2000, 2100 -> WeatherCode.FOG
            4000, 4001, 4200, 4201 -> WeatherCode.RAIN
            5000, 5001, 5100, 5101 -> WeatherCode.SNOW
            6000, 6001, 6200, 6201 -> WeatherCode.SLEET
            7000, 7101, 7102 -> WeatherCode.HAIL
            8000 -> WeatherCode.THUNDERSTORM
            else -> null
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
        return apikey.ifEmpty { BuildConfig.TOMORROW_IO_KEY }
    }

    override val isConfigured
        get() = getApiKeyOrDefault().isNotEmpty()

    override val isRestricted
        get() = apikey.isEmpty()

    override fun getPreferences(context: Context): List<Preference> {
        return listOf(
            EditTextPreference(
                titleId = R.string.settings_weather_source_tomorrow_io_api_key,
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
        private const val TOMORROW_IO_BASE_URL = "https://api.tomorrow.io/"
    }

}
