package ru.skillbranch.kotlinexample

@Suppress("unused")
object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String): String? {
        return map[login.trim()]?.let { user ->
            if (user.checkPassword(password)) user.userInfo else null
        }
    }
}