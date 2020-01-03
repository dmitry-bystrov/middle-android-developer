package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

@Suppress("unused")
class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        @SuppressLint("DefaultLocale")
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login: String? = null
    var login: String
        @SuppressLint("DefaultLocale")
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null
        get() {
            if (field == null) {
                field = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
            }

            return field
        }

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        this.passwordHash = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String,
        import: Boolean = false
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = if (import) mapOf("src" to "csv") else mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        requestAccessCode()
    }

    //for csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String,
        salt: String?
    ) : this(firstName, lastName, email = email, meta = mapOf("src" to "csv")) {
        println("Secondary csv constructor")
        this.salt = salt
        this.passwordHash = password
    }

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun requestAccessCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        phone?.let {
            sendAccessCodeToUser(it, code)
        }
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match current")
    }

    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        fun importUser(csv: String): User {
            val params = csv.split(";")
            val (firstName, lastName) = params[0].trim().fullNameToPair()
            val email: String? = params.getOrNull(1)
            val salt: String? = params.getOrNull(2)?.split(":")?.getOrNull(0)
            val password: String? = params.getOrNull(2)?.split(":")?.getOrNull(1)
            val phone: String? = params.getOrNull(3)

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone, import = true)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password,
                    salt
                )

                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when (size) {
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contains only first name and last name," +
                                    " current split result is $this"
                        )
                    }
                }
        }
    }
}