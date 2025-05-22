package nie.translator.rtranslator.tools

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.supabase.gotrue.Session
import io.supabase.gotrue.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class SessionManager(context: Context) {

    companion object {
        private const val PREF_FILE_NAME = "app_session_prefs"
        private const val KEY_SESSION_DATA = "supabase_session_data"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREF_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Storing the entire session as a JSON string for simplicity.
    // Alternatively, store individual fields like accessToken, refreshToken, userId, expiresAt.
    fun saveSession(session: Session) {
        try {
            val sessionJson = Json.encodeToString(session)
            sharedPreferences.edit().putString(KEY_SESSION_DATA, sessionJson).apply()
        } catch (e: Exception) {
            // Log error or handle, e.g., if serialization fails
            e.printStackTrace()
        }
    }

    fun loadSession(): Session? {
        return try {
            val sessionJson = sharedPreferences.getString(KEY_SESSION_DATA, null)
            if (sessionJson != null) {
                Json.decodeFromString<Session>(sessionJson)
            } else {
                null
            }
        } catch (e: Exception) {
            // Log error or handle, e.g., if deserialization fails or data is corrupt
            e.printStackTrace()
            null
        }
    }

    fun clearSession() {
        sharedPreferences.edit().remove(KEY_SESSION_DATA).apply()
    }
}
