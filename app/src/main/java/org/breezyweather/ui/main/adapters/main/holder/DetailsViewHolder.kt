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

package org.breezyweather.ui.main.adapters.main.holder

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import breezyweather.domain.location.model.Location
import breezyweather.domain.weather.model.Current
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.breezyweather.R
import org.breezyweather.common.basic.BreezyActivity
import org.breezyweather.common.basic.models.options.appearance.DetailDisplay
import org.breezyweather.common.extensions.getFormattedTime
import org.breezyweather.common.extensions.is12Hour
import org.breezyweather.common.extensions.isLandscape
import org.breezyweather.domain.location.model.isDaylight
import org.breezyweather.domain.settings.SettingsManager
import org.breezyweather.ui.main.utils.MainThemeColorProvider
import org.breezyweather.ui.theme.ThemeManager
import org.breezyweather.ui.theme.compose.BreezyWeatherTheme
import org.breezyweather.ui.theme.resource.providers.ResourceProvider
import org.breezyweather.ui.theme.weatherView.WeatherViewController

class DetailsViewHolder(parent: ViewGroup) : AbstractMainCardViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.container_main_details, parent, false)
) {
    private val mTitle: TextView = itemView.findViewById(R.id.container_main_details_title)
    private val mTime: TextView = itemView.findViewById(R.id.container_main_details_time)
    private val mDetailsList: ComposeView = itemView.findViewById(R.id.container_main_details_list)

    override fun onBindView(
        activity: BreezyActivity,
        location: Location,
        provider: ResourceProvider,
        listAnimationEnabled: Boolean,
        itemAnimationEnabled: Boolean,
        firstCard: Boolean,
    ) {
        super.onBindView(activity, location, provider, listAnimationEnabled, itemAnimationEnabled, firstCard)
        location.weather?.let { weather ->
            weather.current?.let { current ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mTitle.isAccessibilityHeading = true
                }
                mTitle.setTextColor(
                    ThemeManager.getInstance(context)
                        .weatherThemeDelegate
                        .getThemeColors(
                            context,
                            WeatherViewController.getWeatherKind(location),
                            WeatherViewController.isDaylight(location)
                        )[0]
                )
                mTime.text = weather.base.forecastUpdateTime?.getFormattedTime(location, context, context.is12Hour)
                mDetailsList.setContent {
                    BreezyWeatherTheme(!MainThemeColorProvider.isLightTheme(context, location)) {
                        ContentView(
                            SettingsManager.getInstance(context).detailDisplayList.toImmutableList(),
                            SettingsManager.getInstance(context).detailDisplayUnlisted.toImmutableList(),
                            current,
                            location
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ContentView(
        detailsInHeaderList: ImmutableList<DetailDisplay>,
        detailsNotInHeaderList: ImmutableList<DetailDisplay>,
        current: Current,
        location: Location,
    ) {
        val context = LocalContext.current
        // TODO: Lazy
        Column {
            availableDetails(
                LocalContext.current,
                detailsInHeaderList,
                detailsNotInHeaderList,
                current,
                location.isDaylight
            ).forEach { detailDisplay ->
                ListItem(
                    modifier = Modifier
                        .clearAndSetSemantics {
                            contentDescription = detailDisplay
                                .getContentDescription(context, current, location.isDaylight)
                                ?: (
                                    detailDisplay.getShortName(context) +
                                        context.getString(R.string.colon_separator) +
                                        detailDisplay.getCurrentValue(context, current, location.isDaylight)!!
                                    )
                        },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    headlineContent = {
                        Text(
                            detailDisplay.getName(context),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            detailDisplay.getCurrentValue(context, current, location.isDaylight)!!,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(
                            painterResource(detailDisplay.iconId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
    }

    companion object {
        fun availableDetails(
            context: Context,
            detailsInHeaderList: List<DetailDisplay>,
            detailsNotInHeaderList: List<DetailDisplay>,
            current: Current,
            isDaylight: Boolean,
        ): List<DetailDisplay> {
            val detailsInHeaderNotNullList = detailsInHeaderList.filter {
                it.getCurrentValue(context, current, isDaylight) != null
            }
            val detailsNotInHeaderNotNullList = detailsNotInHeaderList.filter {
                it.getCurrentValue(context, current, isDaylight) != null
            }
            val nbMaxInHeader = if (context.isLandscape) {
                HeaderViewHolder.NB_CURRENT_ITEMS_LANDSCAPE
            } else {
                HeaderViewHolder.NB_CURRENT_ITEMS_PORTRAIT
            }
            return if (detailsInHeaderNotNullList.size > nbMaxInHeader) {
                detailsInHeaderNotNullList.subList(nbMaxInHeader, detailsInHeaderNotNullList.size) +
                    detailsNotInHeaderNotNullList
            } else {
                detailsNotInHeaderNotNullList
            }
        }
    }
}
