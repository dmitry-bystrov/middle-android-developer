package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

@Suppress("unused")
object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map.containsKey(user.login)) {
                    throw IllegalArgumentException("A user with this email already exists")
                } else {
                    map[user.login] = user
                }
            }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if (!rawPhone.startsWith("+") || rawPhone.contains("[^+\\d( )\\-]".toRegex()) || rawPhone.replace(
                "[^\\d]".toRegex(),
                ""
            ).length != 11
        )
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

        return User.makeUser(fullName, phone = rawPhone)
            .also { user ->
                if (map.containsKey(user.login)) {
                    throw IllegalArgumentException("A user with this phone already exists")
                } else {
                    map[user.login] = user
                }
            }
    }

    fun loginUser(login: String, password: String): String? {
        return when {
            login.startsWith("+") -> loginByPhone(login, password)
            else -> loginByEmail(login, password)
        }
    }

    private fun loginByEmail(login: String, password: String): String? {
        return map[login.trim()]?.let { user ->
            if (user.checkPassword(password)) user.userInfo else null
        }
    }

    private fun loginByPhone(login: String, accessCode: String): String? {
        return map[clearPhone(login)]?.let { user ->
            if (user.checkPassword(accessCode)) user.userInfo else null
        }
    }

    private fun clearPhone(login: String) = login.replace("[^+\\d]".toRegex(), "")

    fun requestAccessCode(login: String) {
        map[clearPhone(login)]?.requestAccessCode()
    }

    fun importUsers(list: List<String>): List<User> {
        val result = mutableListOf<User>()
        list.forEach { csv ->
            result.add(importUser(csv))
        }

        return result
    }

    private fun importUser(csv: String): User {
        return User.importUser(csv)
            .also { user ->
                if (map.containsKey(user.login)) {
                    throw IllegalArgumentException("A user from this csv already exists")
                } else {
                    map[user.login] = user
                }
            }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}