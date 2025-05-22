package nie.translator.rtranslator.tools

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MicrophoneUsageManager(
    private val context: Context,
    private val supabaseManager: SupabaseManager
) {

    companion object {
        private const val PREFS_NAME = "microphone_usage_prefs"
        private const val KEY_USAGE_SINCE_LAST_SYNC_MS = "usage_since_last_sync_ms"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Adds the given duration to the locally accumulated microphone usage.
     * This method is thread-safe.
     * @param durationMillis The duration of microphone usage in milliseconds.
     */
    @Synchronized
    fun addUsage(durationMillis: Long) {
        if (durationMillis <= 0) return
        val currentUsage = getUsageSinceLastSync()
        val newTotalUsage = currentUsage + durationMillis
        sharedPreferences.edit().putLong(KEY_USAGE_SINCE_LAST_SYNC_MS, newTotalUsage).apply()
        // Log.d("MicrophoneUsageManager", "Added usage: $durationMillis ms. New total since sync: $newTotalUsage ms")
    }

    /**
     * Retrieves the accumulated microphone usage since the last successful sync.
     * This method is thread-safe.
     * @return The accumulated usage in milliseconds.
     */
    @Synchronized
    fun getUsageSinceLastSync(): Long {
        return sharedPreferences.getLong(KEY_USAGE_SINCE_LAST_SYNC_MS, 0L)
    }

    /**
     * Resets the locally accumulated microphone usage since the last sync to zero.
     * This method is thread-safe.
     */
    @Synchronized
    private fun resetUsageSinceLastSync() {
        sharedPreferences.edit().putLong(KEY_USAGE_SINCE_LAST_SYNC_MS, 0L).apply()
        // Log.d("MicrophoneUsageManager", "Usage since last sync has been reset.")
    }

    /**
     * Attempts to sync the locally accumulated microphone usage with Supabase.
     * If successful, the local accumulator is reset.
     * This operation is performed on a background thread.
     */
    fun syncWithSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val usageToSync = getUsageSinceLastSync()

            if (usageToSync <= 0) {
                // Log.d("MicrophoneUsageManager", "No usage to sync.")
                return@launch
            }

            // Log.d("MicrophoneUsageManager", "Attempting to sync usage: $usageToSync ms")
            val success = supabaseManager.invokeUpdateMicrophoneUsageRpc(usageToSync)

            if (success) {
                // Log.d("MicrophoneUsageManager", "Successfully synced usage: $usageToSync ms. Resetting local counter.")
                resetUsageSinceLastSync()
            } else {
                // Log.e("MicrophoneUsageManager", "Failed to sync usage: $usageToSync ms.")
                // Optionally, implement retry logic or error handling
            }
        }
    }
}
