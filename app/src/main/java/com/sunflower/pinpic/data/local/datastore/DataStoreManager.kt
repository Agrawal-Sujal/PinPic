package com.sunflower.pinpic.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val PICTURE_URL = stringPreferencesKey("picture_url")
        val USER_ID = stringPreferencesKey("user_id")
        val FULL_NAME = stringPreferencesKey("full_name")
        val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    suspend fun saveAuthInfo(
        jwt: String? = null,
        username: String? = null,
        email: String? = null,
        pictureUrl: String? = null,
        userId: String? = null
    ) {
        context.dataStore.edit { prefs ->
            if (jwt != null) prefs[JWT_TOKEN] = jwt
            if (username != null) prefs[USERNAME] = username
            if (email != null) prefs[EMAIL] = email
            if (pictureUrl != null) prefs[PICTURE_URL] = pictureUrl
            if (userId != null) prefs[USER_ID] = userId
        }
    }

    suspend fun updateProfileInfo(
        fullName: String?,
        email: String?,
        mobileNumber: String?,
        pictureUrl: String?
    ) {
        context.dataStore.edit { prefs ->
            if (fullName != null) prefs[FULL_NAME] = fullName
            if (email != null) prefs[EMAIL] = email
            if (mobileNumber != null) prefs[MOBILE_NUMBER] = mobileNumber
            if (pictureUrl != null) prefs[PICTURE_URL] = pictureUrl
        }
    }

    suspend fun clearAuthInfo() {
        context.dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(EMAIL)
            prefs.remove(PICTURE_URL)
            prefs.remove(FULL_NAME)
            prefs.remove(MOBILE_NUMBER)
            prefs.remove(USER_ID)
        }
    }

    val jwtTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[JWT_TOKEN]
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USERNAME]
    }

    val emailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[EMAIL]
    }

    val pictureUrlFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PICTURE_URL]
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID]
    }

    val fullNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[FULL_NAME]
    }

    val mobileNumberFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[MOBILE_NUMBER]
    }

    val themeModeFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE]
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }
}
