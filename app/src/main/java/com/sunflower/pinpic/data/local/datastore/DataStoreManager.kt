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
    }

    suspend fun saveAuthInfo(jwt: String, username: String?, email: String?, pictureUrl: String?) {
        context.dataStore.edit { prefs ->
            prefs[JWT_TOKEN] = jwt
            if (username != null) prefs[USERNAME] = username
            if (email != null) prefs[EMAIL] = email
            if (pictureUrl != null) prefs[PICTURE_URL] = pictureUrl
        }
    }

    suspend fun clearAuthInfo() {
        context.dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
            prefs.remove(USERNAME)
            prefs.remove(EMAIL)
            prefs.remove(PICTURE_URL)
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
}
