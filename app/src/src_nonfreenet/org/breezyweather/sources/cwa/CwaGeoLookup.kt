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

package org.breezyweather.sources.cwa

import breezyweather.domain.location.model.Location
import com.google.maps.android.SphericalUtil
import com.google.maps.android.model.LatLng

// This function returns the geographically nearest CWA station ID from a list of stations.
// This function is only used for getting UV Index and temperature normals.
fun getNearestStation(location: Location, stationList: Map<String, Map<String, Double>>): String? {
    var distance: Double
    var nearestStation: String? = null
    var nearestDistance: Double

    nearestDistance = Double.POSITIVE_INFINITY
    stationList.forEach { station ->
        distance = SphericalUtil.computeDistanceBetween(
            LatLng(location.latitude, location.longitude),
            LatLng(station.value["lat"] as Double, station.value["lon"] as Double)
        )
        if (distance < nearestDistance) {
            nearestStation = station.key
            nearestDistance = distance
        }
    }
    return nearestStation
}

// Temperature normals are only available at 26 stations (out of 700+).
// They are not available from the main weather API call,
// and must be called with a different endpoint with the exact station ID.
// This list allows matching a location to the nearest of those 26 stations.
val CWA_NORMALS_STATIONS = mapOf<String, Map<String, Double>>(
    "466900" to mapOf("lat" to 25.164888, "lon" to 121.448906),
    "466920" to mapOf("lat" to 25.037659, "lon" to 121.514854),
    "467610" to mapOf("lat" to 23.097486, "lon" to 121.37343),
    "466940" to mapOf("lat" to 25.133314, "lon" to 121.74048),
    "467300" to mapOf("lat" to 23.25695, "lon" to 119.667465),
    "467620" to mapOf("lat" to 22.036968, "lon" to 121.55834),
    "466990" to mapOf("lat" to 23.975128, "lon" to 121.61327),
    "467540" to mapOf("lat" to 22.355675, "lon" to 120.903786),
    "467440" to mapOf("lat" to 22.565992, "lon" to 120.315735),
    "467660" to mapOf("lat" to 22.75221, "lon" to 121.15459),
    "467480" to mapOf("lat" to 23.495926, "lon" to 120.43291),
    "467060" to mapOf("lat" to 24.596737, "lon" to 121.85737),
    "467080" to mapOf("lat" to 24.763975, "lon" to 121.75653),
    "466910" to mapOf("lat" to 25.182587, "lon" to 121.52973),
    "466930" to mapOf("lat" to 25.162079, "lon" to 121.54455),
    "466950" to mapOf("lat" to 25.627975, "lon" to 122.07974),
    "467410" to mapOf("lat" to 22.993238, "lon" to 120.20477),
    "466880" to mapOf("lat" to 24.997646, "lon" to 121.44202),
    "467650" to mapOf("lat" to 23.881325, "lon" to 120.90805),
    "467530" to mapOf("lat" to 23.508207, "lon" to 120.81324),
    "467571" to mapOf("lat" to 24.827852, "lon" to 121.01422),
    "467550" to mapOf("lat" to 23.487614, "lon" to 120.95952),
    "467770" to mapOf("lat" to 24.256002, "lon" to 120.523384),
    "467350" to mapOf("lat" to 23.565502, "lon" to 119.563095),
    "467490" to mapOf("lat" to 24.145737, "lon" to 120.684074),
    "467590" to mapOf("lat" to 22.003897, "lon" to 120.74634)
)

// UV Index observations are only available at 29 stations (out of 700+).
// They are not available from the main weather API call,
// and must be called with a different endpoint with the exact station ID.
// This list allows matching a location to the nearest of those 29 stations.
val CWA_UV_STATIONS = mapOf<String, Map<String, Double>>(
    "466940" to mapOf("lat" to 25.133314, "lon" to 121.74048),
    "466900" to mapOf("lat" to 25.164888, "lon" to 121.448906),
    "466881" to mapOf("lat" to 24.959328, "lon" to 121.52433),
    "466930" to mapOf("lat" to 25.162079, "lon" to 121.54455),
    "466910" to mapOf("lat" to 25.182587, "lon" to 121.52973),
    "467571" to mapOf("lat" to 24.827852, "lon" to 121.01422),
    "467490" to mapOf("lat" to 24.145737, "lon" to 120.684074),
    "467350" to mapOf("lat" to 23.565502, "lon" to 119.563095),
    "467650" to mapOf("lat" to 23.881325, "lon" to 120.90805),
    "467530" to mapOf("lat" to 23.508207, "lon" to 120.81324),
    "467550" to mapOf("lat" to 23.487614, "lon" to 120.95952),
    "467480" to mapOf("lat" to 23.495926, "lon" to 120.43291),
    "467410" to mapOf("lat" to 22.993238, "lon" to 120.20477),
    "467441" to mapOf("lat" to 22.730433, "lon" to 120.312515),
    "467590" to mapOf("lat" to 22.003897, "lon" to 120.74634),
    "467080" to mapOf("lat" to 24.763975, "lon" to 121.75653),
    "466990" to mapOf("lat" to 23.975128, "lon" to 121.61327),
    "467610" to mapOf("lat" to 23.097486, "lon" to 121.37343),
    "467660" to mapOf("lat" to 22.75221, "lon" to 121.15459),
    "467540" to mapOf("lat" to 22.355675, "lon" to 120.903786),
    "467620" to mapOf("lat" to 22.036968, "lon" to 121.55834),
    "466950" to mapOf("lat" to 25.627975, "lon" to 122.07974),
    "467300" to mapOf("lat" to 23.25695, "lon" to 119.667465),
    "467420" to mapOf("lat" to 23.038385, "lon" to 120.2367),
    "466920" to mapOf("lat" to 25.037659, "lon" to 121.514854),
    "467110" to mapOf("lat" to 24.407307, "lon" to 118.28928),
    "467990" to mapOf("lat" to 26.16927, "lon" to 119.923416),
    "467050" to mapOf("lat" to 25.006744, "lon" to 121.047485),
    "467270" to mapOf("lat" to 23.873802, "lon" to 120.58128)
)

// CWA issues warnings for different counties and specific townships classified as:
//  • Mountain ("M"): 59 townships
//  • Keelung North Coast ("K"): 15 townships
//  • Hengchun Peninsula ("H"): 6 townships
//  • Lanyu and Ludao ("L"): 2 townships
//
// Source of township specification (last checked 2024-25-29):
// https://www.cwa.gov.tw/Data/js/info/Info_Warning.js
//
// (Township codes in this list have been normalized
//  to match results from the reverse geocoding call.)
val CWA_TOWNSHIP_WARNING_AREAS = mapOf<String, String>(
    "10002110" to "M",
    "10002120" to "M",
    "10004080" to "M",
    "10004090" to "M",
    "10004120" to "M",
    "10004130" to "M",
    "10005070" to "M",
    "10005080" to "M",
    "10005110" to "M",
    "10005170" to "M",
    "10005180" to "M",
    "10008020" to "M",
    "10008040" to "M",
    "10008070" to "M",
    "10008090" to "M",
    "10008100" to "M",
    "10008110" to "M",
    "10008120" to "M",
    "10008130" to "M",
    "10009070" to "M",
    "10010140" to "M",
    "10010150" to "M",
    "10010160" to "M",
    "10010170" to "M",
    "10010180" to "M",
    "10013040" to "H",
    "10013230" to "H",
    "10013240" to "H",
    "10013250" to "H",
    "10013260" to "M",
    "10013270" to "M",
    "10013280" to "M",
    "10013290" to "M",
    "10013300" to "M",
    "10013310" to "M",
    "10013320" to "H",
    "10013330" to "H",
    "10014040" to "M",
    "10014110" to "L",
    "10014120" to "M",
    "10014130" to "M",
    "10014140" to "M",
    "10014150" to "M",
    "10014160" to "L",
    "10015110" to "M",
    "10015120" to "M",
    "10015130" to "M",
    "10017010" to "K",
    "10017020" to "K",
    "10017030" to "K",
    "10017040" to "K",
    "10017050" to "K",
    "10017060" to "K",
    "10017070" to "K",
    "63000110" to "M",
    "63000120" to "M",
    "64000320" to "M",
    "64000330" to "M",
    "64000360" to "M",
    "64000370" to "M",
    "64000380" to "M",
    "65000090" to "M",
    "65000100" to "K",
    "65000120" to "K",
    "65000190" to "M",
    "65000200" to "M",
    "65000210" to "K",
    "65000220" to "K",
    "65000240" to "M",
    "65000250" to "K",
    "65000260" to "K",
    "65000270" to "K",
    "65000280" to "K",
    "65000290" to "M",
    "66000100" to "M",
    "66000190" to "M",
    "66000200" to "M",
    "66000270" to "M",
    "66000290" to "M",
    "67000240" to "M",
    "67000250" to "M",
    "68000130" to "M"
)