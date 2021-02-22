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

package me.proton.core.user.data.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import me.proton.core.account.data.repository.AccountRepositoryImpl
import me.proton.core.accountmanager.data.AccountManagerImpl
import me.proton.core.accountmanager.data.db.AccountManagerDatabase
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.crypto.android.context.AndroidCryptoContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.Product
import me.proton.core.key.data.api.response.AddressesResponse
import me.proton.core.key.data.api.response.UsersResponse
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.decryptTextOrNull
import me.proton.core.key.domain.encryptText
import me.proton.core.key.domain.useKeys
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.data.di.ApiFactory
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.runBlockingWithTimeout
import me.proton.core.user.data.TestAccountManagerDatabase
import me.proton.core.user.data.TestAccounts
import me.proton.core.user.data.TestAddresses
import me.proton.core.user.data.TestApiManager
import me.proton.core.user.data.TestUsers
import me.proton.core.user.data.UserAddressKeySecretProvider
import me.proton.core.user.data.api.AddressApi
import me.proton.core.user.data.api.UserApi
import me.proton.core.user.domain.extension.primary
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserAddressRepositoryImplTests {

    private val sessionProvider = mockk<SessionProvider>(relaxed = true)
    private val apiFactory = mockk<ApiFactory>(relaxed = true)

    private val userApi = mockk<UserApi>(relaxed = true)
    private val addressApi = mockk<AddressApi>(relaxed = true)

    private val cryptoContext: CryptoContext = AndroidCryptoContext(
        keyStoreCrypto = object : KeyStoreCrypto {
            override fun encrypt(value: String): EncryptedString = value
            override fun decrypt(value: EncryptedString): String = value
            override fun encrypt(value: PlainByteArray): EncryptedByteArray = EncryptedByteArray(value.array.copyOf())
            override fun decrypt(value: EncryptedByteArray): PlainByteArray = PlainByteArray(value.array.copyOf())
        }
    )

    private lateinit var apiProvider: ApiProvider

    private lateinit var accountManager: AccountManager

    private lateinit var db: AccountManagerDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var userAddressRepository: UserAddressRepositoryImpl
    private lateinit var userAddressKeySecretProvider: UserAddressKeySecretProvider

    @Before
    fun setup() {
        // Build a new fresh in memory Database, for each test.
        db = TestAccountManagerDatabase.buildMultiThreaded()

        coEvery { sessionProvider.getSessionId(any()) } returns TestAccounts.sessionId
        every { apiFactory.create(any(), interfaceClass = UserApi::class) } returns TestApiManager(userApi)
        every { apiFactory.create(any(), interfaceClass = AddressApi::class) } returns TestApiManager(addressApi)

        apiProvider = ApiProvider(apiFactory, sessionProvider)

        userRepository = UserRepositoryImpl(db, apiProvider)

        userAddressKeySecretProvider = UserAddressKeySecretProvider(
            userRepository,
            userRepository,
            cryptoContext
        )

        userAddressRepository = UserAddressRepositoryImpl(
            db,
            apiProvider,
            userRepository,
            userAddressKeySecretProvider
        )

        // Needed to addAccount (User.userId foreign key -> Account.userId).
        accountManager = AccountManagerImpl(
            Product.Mail,
            AccountRepositoryImpl(Product.Mail, db, cryptoContext.keyStoreCrypto),
            mockk(relaxed = true)
        )

        // Before fetching any User, account need to be added to AccountManager (if not -> foreign key exception).
        runBlocking {
            accountManager.addAccount(TestAccounts.User1.account, TestAccounts.session)
            accountManager.addAccount(TestAccounts.User2.account, TestAccounts.session)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAddresses() = runBlockingWithTimeout {
        // GIVEN
        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User1.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User1.Address1.response))
        }

        // WHEN
        val addresses = userAddressRepository.getAddressesFlow(TestUsers.User1.id, refresh = true)
            .mapLatest { it as? DataResult.Success }
            .mapLatest { it?.value }
            .filter { it?.isNotEmpty() ?: false }
            .filterNotNull()
            .firstOrNull()

        // THEN
        assertNotNull(addresses)
        assertEquals(1, addresses.size)
    }

    @Test
    fun getAddressesBlocking() = runBlockingWithTimeout {
        // GIVEN
        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User1.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User1.Address1.response))
        }

        // WHEN
        val user = userRepository.getUser(TestUsers.User1.id, refresh = true)
        val addresses = userAddressRepository.getAddresses(TestUsers.User1.id, refresh = true)

        // THEN
        assertNotNull(addresses)
        assertEquals(1, addresses.size)
    }

    @Test
    fun getAddressesBlocking_useKeys_userLocked() = runBlockingWithTimeout {
        // GIVEN
        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User1.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User1.Address1.response))
        }

        // WHEN
        val user = userRepository.getUser(TestUsers.User1.id, refresh = true)
        val addresses = userAddressRepository.getAddresses(TestUsers.User1.id, refresh = true)

        // THEN
        addresses.primary()!!.useKeys(cryptoContext) {
            val message = "message"

            val encryptedText = encryptText(message)

            assertFailsWith(CryptoException::class) { decryptText(encryptedText) }
            assertNull(decryptTextOrNull(encryptedText))
        }
    }

    @Test
    fun getAddressesBlocking_useKeys_userUnLocked() = runBlockingWithTimeout {
        // GIVEN
        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User1.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User1.Address1.response))
        }

        // Fetch User (add to cache/DB) and set passphrase -> unlock User.
        userRepository.getUser(TestUsers.User1.id)
        userRepository.setPassphrase(TestUsers.User1.id, TestUsers.User1.Key1.passphrase)

        // WHEN
        val user = userRepository.getUser(TestUsers.User1.id, refresh = true)
        val addresses = userAddressRepository.getAddresses(TestUsers.User1.id, refresh = true)

        // THEN
        addresses.primary()!!.useKeys(cryptoContext) {
            val message = "message"

            val encryptedText = encryptText(message)
            val decryptedText = decryptText(encryptedText)

            assertEquals(message, decryptedText)
        }
    }

    @Test
    fun getAddressesBlocking_useKeys_userUnLocked_token_signature() = runBlockingWithTimeout {
        // GIVEN
        val encryptedWithTestKeysKey1PrivateKey =
            """
            -----BEGIN PGP MESSAGE-----
            Version: ProtonMail

            wcBMA5kajsUECZmgAQgAgJuGP/0+pUPu24mWeviRQ79s6fKKsKh6y1aBXwJM
            eQ8mSaLvHNSaCa8s9yozs9gWo2/Uf8Lpmqb70SMh2npwI5hyOFqXsrMEoEHn
            KTf86kSHnGZEtwrScXnekJjO1rfYynnAYuppTfpUc2E/uGZg6RChlwPbBZMw
            tOk8n6iL6u0+Ren9fxAmmMTw66vc5PDejmfAgzbdxeD7qV8wzqmipgiErk/w
            dPEzI5QGtGXUwsDfJeSGEdCslN1kHtZRj2B3tg6Ms7Ea/VIb3Kq6uyn2hQhS
            MlWwjzauF5mryV4Kbi1RP6yTykbPnRz6ia22HwbWzOVJ2Nu534RqNYA/99Bd
            G9JcAXjM6al21XdX0ZQww2R0Of3VzFVwQX+RSG1SWGq11u2nu5iXBVUJDa5x
            MS2SksqmW3Bh7Tbz2zlrCNZxH8USiAxXt/3xjwNlXgCg4b8sKNHNN4+Wa6S8
            HNwbYAc=
            =9RxF
            -----END PGP MESSAGE-----
            """.trimIndent()

        val expected = "Test PGP/MIME Message\n\n\n"

        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User2.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User2.Address1.response))
        }

        // Fetch User (add to cache/DB) and set passphrase -> unlock User.
        userRepository.getUser(TestUsers.User2.id)
        userRepository.setPassphrase(TestUsers.User2.id, TestUsers.User2.Key1.passphrase)

        // WHEN
        val user = userRepository.getUser(TestUsers.User2.id, refresh = true)
        val addresses = userAddressRepository.getAddresses(TestUsers.User2.id, refresh = true)

        // THEN
        addresses.primary()!!.useKeys(cryptoContext) {
            val message = "message"

            val encryptedText = encryptText(message)
            val decryptedText = decryptText(encryptedText)

            assertEquals(message, decryptedText)

            val decryptedWithAddressPrivateKey = decryptText(encryptedWithTestKeysKey1PrivateKey)
            assertEquals(expected, decryptedWithAddressPrivateKey)
        }
    }

    @Test
    fun getAddressesBlocking_useKeys_userLocked_token_signature() = runBlockingWithTimeout {
        // GIVEN
        coEvery { userApi.getUsers() } answers {
            UsersResponse(TestUsers.User2.response)
        }
        coEvery { addressApi.getAddresses() } answers {
            AddressesResponse(listOf(TestAddresses.User2.Address1.response))
        }

        // WHEN
        val user = userRepository.getUser(TestUsers.User2.id, refresh = true)
        val addresses = userAddressRepository.getAddresses(TestUsers.User2.id, refresh = true)

        // THEN
        addresses.primary()!!.useKeys(cryptoContext) {
            val message = "message"

            val encryptedText = encryptText(message)

            assertFailsWith(CryptoException::class) { decryptText(encryptedText) }
            assertNull(decryptTextOrNull(encryptedText))
        }
    }
}
