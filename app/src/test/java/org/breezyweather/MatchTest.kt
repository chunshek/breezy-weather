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

package org.breezyweather

import kotlinx.coroutines.test.runTest
import org.breezyweather.common.basic.models.options.basic.Utils
import org.junit.jupiter.api.Test

class MatchTest {

    @Test
    fun split() = runTest {
        val text = "dadasd dsad   dad"
        println(text.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().contentToString())
    }

    @Test
    fun formatFloat() = runTest {
        println(Utils.formatDouble(7.00646, 2))
        println(Utils.formatDouble(7.00246, 2))
    }
}
