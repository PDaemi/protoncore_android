/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.plan.domain.entity

import me.proton.core.domain.entity.AppStore
import java.time.Instant

data class DynamicPlanInstance(
    val id: String,
    val cycle: Int,
    val description: String,
    val periodEnd: Instant,
    /** Map<Currency, DynamicPlanPrice> */
    val price: Map<String, DynamicPlanPrice>,
    val vendors: Map<AppStore, DynamicPlanVendor> = emptyMap()
)
