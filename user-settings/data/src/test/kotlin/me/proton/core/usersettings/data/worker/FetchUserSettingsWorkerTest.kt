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

package me.proton.core.usersettings.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.usersettings.domain.usecase.GetUserSettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals


@RunWith(RobolectricTestRunner::class)
class FetchUserSettingsWorkerTest {

    internal lateinit var getUserSettings: GetUserSettings

    private lateinit var context: Context

    private val userId = UserId("user-id")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        getUserSettings = mockk(relaxUnitFun = true)
    }

    @Test
    fun success() = runTest {
        coEvery { getUserSettings(userId, true) } returns mockk()

        val result = makeWorker(userId).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun retry() = runTest {
        coEvery { getUserSettings(userId, true) } throws ApiException(ApiResult.Error.NoInternet())

        val result = makeWorker(userId).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun failure() = runTest {
        coEvery { getUserSettings(userId, true) } throws IllegalStateException()

        val result = makeWorker(userId).doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    private fun makeWorker(userId: UserId): FetchUserSettingsWorker =
        TestListenableWorkerBuilder<FetchUserSettingsWorker>(context)
            // hilt is not working for this test
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ) = FetchUserSettingsWorker(appContext, workerParameters, getUserSettings)

            })
            .setInputData(FetchUserSettingsWorker.getWorkData(userId))
            .build()

}
