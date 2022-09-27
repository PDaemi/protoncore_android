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

package me.proton.core.test.android.instrumented.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import me.proton.core.test.android.instrumented.ProtonTest.Companion.getTargetContext

object StringUtils {

    private val lettersAndNumbers = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    private const val alphaNumericWithSpecialCharacters =
        "abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@+_)(*&^%$#@!"
    private const val emailCharacters =
        "abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!#\$%&'*+-=?^_`{|}~"

    fun stringFromResource(@StringRes id: Int, vararg formatArgs: Any): String = if (formatArgs.isEmpty()) {
        getTargetContext().resources.getString(id)
    } else {
        getTargetContext().resources.getString(id, *formatArgs)
    }

    fun stringFromResource(@StringRes id: Int): String =
        getTargetContext().resources.getString(id)

    fun pluralStringFromResource(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty()) {
            getTargetContext().resources.getQuantityString(id, quantity)
        } else {
            getTargetContext().resources.getQuantityString(id, quantity, *formatArgs)
        }

    fun getAlphaNumericStringWithSpecialCharacters(length: Long = 10): String =
        randomString(length, alphaNumericWithSpecialCharacters.toCharArray().toList())

    fun getEmailString(length: Long = 10): String =
        randomString(length, emailCharacters.toCharArray().toList())

    fun randomString(stringLength: Long = 10, source: List<Char> = lettersAndNumbers.toCharArray().toList()): String {
        return (1..stringLength)
            .map { kotlin.random.Random.nextInt(0, source.size) }
            .map(source::get)
            .joinToString("")
    }
}
