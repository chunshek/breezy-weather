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

package org.breezyweather.common.source

import android.content.Context
import androidx.compose.runtime.Composable
import breezyweather.domain.location.model.Location
import breezyweather.domain.source.SourceFeature
import kotlinx.collections.immutable.ImmutableList

/**
 * Implement this if you need parameters specific to each location
 * Use ConfigurableSource instead if you need all locations parameters
 */
interface PreferencesParametersSource : Source {

    /**
     * Must return true if the preferences screen is enabled for the given parameters
     *
     * Parameters:
     * - the location
     * - list of features requested. Empty if not specific to a feature
     */
    fun hasPreferencesScreen(
        location: Location,
        features: List<SourceFeature> = emptyList(),
    ): Boolean

    @Composable
    fun PerLocationPreferences(
        context: Context,
        location: Location,
        features: ImmutableList<SourceFeature>,
        onSave: (Map<String, String>) -> Unit,
    )
}
