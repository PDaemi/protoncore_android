/*
 * Copyright (c) 2022 Proton Technologies AG
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

package me.proton.core.featureflag.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.FeatureId
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository
import javax.inject.Inject

public class FeatureFlagManagerImpl @Inject internal constructor(
    private val repository: FeatureFlagRepository
) : FeatureFlagManager {

    override fun observe(
        userId: UserId?,
        featureId: FeatureId,
        refresh: Boolean
    ): Flow<FeatureFlag?> = repository.observe(userId, featureId, refresh).distinctUntilChanged()

    override fun observeOrDefault(
        userId: UserId?,
        featureId: FeatureId,
        default: FeatureFlag,
        refresh: Boolean
    ): Flow<FeatureFlag> = observe(userId, featureId, refresh).map { it ?: default }

    override suspend fun get(
        userId: UserId?,
        featureId: FeatureId,
        refresh: Boolean
    ): FeatureFlag? = repository.get(userId, featureId, refresh)

    override suspend fun getOrDefault(
        userId: UserId?,
        featureId: FeatureId,
        default: FeatureFlag,
        refresh: Boolean
    ): FeatureFlag = runCatching { get(userId, featureId, refresh) }.getOrNull() ?: default

    override fun prefetch(
        userId: UserId?,
        featureIds: Set<FeatureId>
    ): Unit = repository.prefetch(userId, featureIds)
}
