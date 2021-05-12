/*
 * Copyright (c) 2020 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
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

package me.proton.core.humanverification.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.proton.core.humanverification.domain.HumanVerificationWorkflowHandler
import me.proton.core.humanverification.presentation.exception.NotEnoughVerificationOptions
import me.proton.core.humanverification.presentation.ui.HumanVerificationDialogFragment
import me.proton.core.network.domain.session.ClientId
import me.proton.core.presentation.viewmodel.ProtonViewModel
import me.proton.core.user.domain.entity.UserVerificationTokenType
import javax.inject.Inject

/**
 * View model class to serve the main Human Verification screen.
 */
@HiltViewModel
class HumanVerificationViewModel @Inject constructor(
    private val humanVerificationWorkflowHandler: HumanVerificationWorkflowHandler,
    private val savedStateHandle: SavedStateHandle
) : ProtonViewModel() {

    private lateinit var currentActiveVerificationMethod: UserVerificationTokenType

    private var availableVerificationMethods: List<String> =
        savedStateHandle.get<List<String>>(HumanVerificationDialogFragment.ARG_VERIFICATION_OPTIONS)!!

    private val _activeMethod = MutableStateFlow<String?>(null)
    private val _enabledMethods = MutableStateFlow<List<String>>(emptyList())

    val activeMethod = _activeMethod.asStateFlow()
    val enabledMethods = _enabledMethods.asStateFlow()

    init {
        // A list of all available methods that the API is currently supporting for this particular user and device.
        // The UI should present the verification methods for each one of them.
        // It is safe to use !! here, guaranteed that there will be at least 1 verification method available
        if (availableVerificationMethods.isEmpty()) {
            throw NotEnoughVerificationOptions("Please provide at least 1 verification method")
        }

        _enabledMethods.tryEmit(availableVerificationMethods)
        defineActiveVerificationMethod()
    }

    /**
     * Sets the currently active verification method that the user chose.
     */
    fun defineActiveVerificationMethod(userSelectedMethod: UserVerificationTokenType? = null) {
        userSelectedMethod?.let {
            currentActiveVerificationMethod = it
        } ?: run {
            currentActiveVerificationMethod =
                UserVerificationTokenType.fromString(availableVerificationMethods.sorted()[0])
        }
        _activeMethod.tryEmit(currentActiveVerificationMethod.tokenTypeValue)
    }

    fun onHumanVerificationSuccess(clientId: ClientId, tokenType: String?, tokenCode: String?): Job {
        return if (!tokenType.isNullOrBlank() && !tokenCode.isNullOrBlank()) {
            viewModelScope.launch {
                humanVerificationWorkflowHandler.handleHumanVerificationSuccess(
                    clientId = clientId,
                    tokenType = tokenType,
                    tokenCode = tokenCode
                )
            }
        } else {
            viewModelScope.launch {
                humanVerificationWorkflowHandler.handleHumanVerificationFailed(clientId = clientId)
            }
        }
    }

    fun onHumanVerificationCanceled(clientId: ClientId) =
        viewModelScope.launch {
            humanVerificationWorkflowHandler.handleHumanVerificationFailed(clientId = clientId)
            humanVerificationWorkflowHandler.handleHumanVerificationCanceled(clientId = clientId)
        }
}
