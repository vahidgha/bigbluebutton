package org.bigbluebutton.android

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Remembers form values; the shared secret lives in encrypted storage. */
class Prefs(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "bbb_secure_prefs", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { context.getSharedPreferences("bbb_prefs_fallback", Context.MODE_PRIVATE) }

    var link by string("link")
    var server by string("server")
    var secret by string("secret")
    var meetingId by string("meetingId")
    var meetingName by string("meetingName")
    var userName by string("userName")
    var moderator: Boolean
        get() = prefs.getBoolean("moderator", false)
        set(v) = prefs.edit().putBoolean("moderator", v).apply()
    var serverMode: Boolean
        get() = prefs.getBoolean("serverMode", false)
        set(v) = prefs.edit().putBoolean("serverMode", v).apply()

    private fun string(key: String) = object : kotlin.properties.ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = prefs.getString(key, "") ?: ""
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) {
            prefs.edit().putString(key, value).apply()
        }
    }
}
