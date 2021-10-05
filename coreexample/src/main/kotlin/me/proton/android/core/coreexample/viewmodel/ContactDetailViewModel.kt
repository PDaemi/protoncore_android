/*
 * Copyright (c) 2021 Proton Technologies AG
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

package me.proton.android.core.coreexample.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import me.proton.android.core.coreexample.utils.createToBeEncryptedAndSignedVCard
import me.proton.android.core.coreexample.utils.createToBeSignedVCard
import me.proton.android.core.coreexample.utils.prettyPrint
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.contact.domain.encryptAndSignContactCard
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.contact.domain.entity.ContactWithCards
import me.proton.core.contact.domain.repository.ContactRepository
import me.proton.core.contact.domain.signContactCard
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val accountManager: AccountManager,
    private val contactRepository: ContactRepository,
    private val userManager: UserManager,
    private val cryptoContext: CryptoContext,
) : ViewModel() {

    private val mutableViewState = MutableStateFlow<ViewState?>(null)
    private val mutableLoadingState = MutableStateFlow(false)
    private val mutableViewEvent = MutableSharedFlow<ViewEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val viewState = mutableViewState.asStateFlow().filterNotNull()
    val loadingState = mutableLoadingState.asStateFlow()
    val viewEvent = mutableViewEvent.asSharedFlow()

    private val contactId: ContactId = ContactId(savedStateHandle.get(ARG_CONTACT_ID)!!)
    private var observeContactJob: Job? = null

    init {
        CoreLogger.d("contact", "presenting contact $contactId")
        observeContact()
    }

    private fun observeContact() {
        cancelObserveContact()
        observeContactJob = viewModelScope.launch {
            accountManager.getPrimaryUserId().filterNotNull()
                .flatMapLatest { contactRepository.observeContactWithCards(it, contactId) }
                .collect { result -> handleResult(result) }
        }
    }

    private fun cancelObserveContact() {
        observeContactJob?.cancel()
        observeContactJob = null
    }

    private fun handleResult(result: DataResult<ContactWithCards>) {
        when (result) {
            is DataResult.Error -> handleDataResultError(result)
            is DataResult.Processing -> { /* no-op */ }
            is DataResult.Success -> {
                mutableViewState.value = ViewState(result.value.prettyPrint())
            }
        }.exhaustive
    }

    private fun handleDataResultError(error: DataResult.Error) {
        val errorMessage = error.message ?: "Unknown error"
        val errorCause = error.cause ?: Throwable(errorMessage)
        handleError(errorCause, errorMessage)
    }

    private fun handleError(throwable: Throwable, errorMessage: String) {
        CoreLogger.e("contact", throwable, errorMessage)
        mutableLoadingState.value = false
        mutableViewEvent.tryEmit(ViewEvent.Error(errorMessage))
    }

    fun deleteContact() {
        mutableLoadingState.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                cancelObserveContact()
                val userId = accountManager.getPrimaryUserId().filterNotNull().first()
                contactRepository.deleteContacts(userId, listOf(contactId))
                mutableViewEvent.tryEmit(ViewEvent.Success)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                handleError(throwable, "Error deleting contact $contactId")
            }
        }
    }

    fun updateContact() {
        viewModelScope.launch(Dispatchers.Default) {
            mutableLoadingState.value = true
            try {
                val userId = accountManager.getPrimaryUserId().filterNotNull().first()
                val user = userManager.getUser(userId)
                val contactWithCards = contactRepository.observeContactWithCards(userId, contactId)
                    .mapSuccessValueOrNull()
                    .filterNotNull()
                    .first()
                contactWithCards.contactCards
                val cards = listOf(
                    user.signContactCard(cryptoContext, createToBeSignedVCard(contactWithCards.contact.name)),
                    user.encryptAndSignContactCard(
                        cryptoContext,
                        createToBeEncryptedAndSignedVCard(contactWithCards.contact.name)
                    )
                )
                contactRepository.updateContact(userId, contactId, cards)
                mutableLoadingState.value = false
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                handleError(throwable, "Error updating contact $contactId")
            }
        }
    }

    data class ViewState(val contact: String)

    sealed class ViewEvent {
        data class Error(val reason: String) : ViewEvent()
        object Success : ViewEvent()
    }

    companion object {
        const val ARG_CONTACT_ID = "arg.contactId"
    }
}
