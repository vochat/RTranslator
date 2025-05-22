package nie.translator.rtranslator.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class SubscriptionData(
    val isSubscribed: Boolean,
    val subscriptionExpiryEpochMillis: Long?,
    val dailyMicrophoneUsageMs: Long,
    val lastUsageResetEpochMillis: Long?,
    val fetchedAtMillis: Long // For internal cache validation
) {
    fun isSubscriptionActive(): Boolean {
        return isSubscribed && (subscriptionExpiryEpochMillis == null || subscriptionExpiryEpochMillis > System.currentTimeMillis())
    }

    fun isDailyLimitExceeded(dailyLimitMs: Long): Boolean {
        if (isSubscriptionActive()) return false // No limit for subscribed users

        val todayEpochMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val lastResetDateMatchesToday = lastUsageResetEpochMillis != null && lastUsageResetEpochMillis >= todayEpochMillis

        return if (lastResetDateMatchesToday) {
            dailyMicrophoneUsageMs >= dailyLimitMs
        } else {
            // If last reset date is not today, usage should be considered 0 for today,
            // unless it's an old record and backend hasn't reset it (which should be handled by backend logic or RPC).
            // For client-side check, if not reset today, assume it's not exceeded yet for *today's* quota.
            // This part of logic depends on how backend/RPC resets daily_microphone_usage_ms and last_usage_reset_date.
            // If update_microphone_usage RPC handles resetting daily usage if date changed, then this is fine.
            false // Or handle more complex logic if daily reset is purely client-driven (not recommended)
        }
    }
}

class SubscriptionManager(private val supabaseManager: SupabaseManager) {

    private val _subscriptionData = MutableStateFlow<SubscriptionData?>(null)
    val subscriptionData: StateFlow<SubscriptionData?> = _subscriptionData.asStateFlow()

    private var lastFetchedTimeMillis: Long = 0L
    private val CACHE_DURATION_MS: Long = 5 * 60 * 1000 // 5 minutes, adjust as needed

    private val refreshMutex = Mutex()

    companion object {
        const val FREE_TIER_DAILY_LIMIT_MS: Long = 60 * 60 * 1000 // 60 minutes
    }

    init {
        // Optionally, trigger a refresh when the app starts or when a user logs in.
        // This can be done by observing auth state changes if SupabaseManager provides such a mechanism.
        // For now, it will refresh on first call to getSubscriptionData or explicitly via refresh.
    }

    private fun parseIsoDateStringToEpochMillis(dateString: String?): Long? {
        if (dateString == null) return null
        return try {
            Instant.parse(dateString).toEpochMilli()
        } catch (e: DateTimeParseException) {
            // Fallback for date only strings (YYYY-MM-DD), assuming UTC start of day
            try {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                         .atStartOfDay(ZoneOffset.UTC)
                         .toInstant()
                         .toEpochMilli()
            } catch (e2: DateTimeParseException) {
                e.printStackTrace()
                e2.printStackTrace()
                null
            }
        }
    }
    
    suspend fun refreshSubscriptionData(): SubscriptionData? {
        return refreshMutex.withLock {
            // Double-check if another coroutine refreshed while waiting for the mutex
            if (System.currentTimeMillis() - lastFetchedTimeMillis < CACHE_DURATION_MS / 2 && _subscriptionData.value != null) {
                 // Recently refreshed by another call, return current data
                return@withLock _subscriptionData.value
            }

            val userProfile = supabaseManager.fetchUserProfile()
            if (userProfile != null) {
                val newData = SubscriptionData(
                    isSubscribed = userProfile.isSubscribed,
                    subscriptionExpiryEpochMillis = parseIsoDateStringToEpochMillis(userProfile.subscriptionExpiryDate),
                    dailyMicrophoneUsageMs = userProfile.dailyMicrophoneUsageMs,
                    lastUsageResetEpochMillis = parseIsoDateStringToEpochMillis(userProfile.lastUsageResetDate),
                    fetchedAtMillis = System.currentTimeMillis()
                )
                _subscriptionData.value = newData
                lastFetchedTimeMillis = newData.fetchedAtMillis
                newData
            } else {
                // Failed to fetch, could clear cache or keep stale, or return specific error
                // For now, nullify if fetch fails hard, or keep stale if that's preferred.
                // _subscriptionData.value = null 
                // lastFetchedTimeMillis = 0L // Force refresh next time
                _subscriptionData.value // Return current (possibly stale or null) data
            }
        }
    }


    suspend fun getSubscriptionData(forceRefresh: Boolean = false): SubscriptionData? {
        val currentTime = System.currentTimeMillis()
        val isCacheStale = (currentTime - lastFetchedTimeMillis) > CACHE_DURATION_MS
        val currentData = _subscriptionData.value

        return if (forceRefresh || isCacheStale || currentData == null) {
            refreshSubscriptionData()
        } else {
            currentData
        }
    }

    /**
     * Checks if the microphone can be activated based on subscription status and usage limits.
     * This will also refresh subscription data if it's stale or forced.
     * @param forceRefresh Force refresh of subscription data from backend.
     * @return true if mic can be activated, false otherwise.
     */
    suspend fun canActivateMicrophone(forceRefresh: Boolean = false): Boolean {
        val data = getSubscriptionData(forceRefresh) ?: return false // If no data, assume no activation

        if (data.isSubscriptionActive()) {
            return true // Subscribed users can always activate
        }

        // For free tier users, check daily limit
        // The daily_microphone_usage_ms should be reset by the backend or RPC daily.
        // The 'update_microphone_usage' RPC should ideally handle the reset if 'last_usage_reset_date' is not today.
        // Or, a separate daily job on the backend.
        // Client-side check for reset:
        val today = LocalDate.now(ZoneOffset.UTC)
        val lastResetDate = data.lastUsageResetEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

        return if (lastResetDate != null && lastResetDate.isEqual(today)) {
            // Usage has been reset today, check current usage against limit
            data.dailyMicrophoneUsageMs < FREE_TIER_DAILY_LIMIT_MS
        } else if (lastResetDate == null || lastResetDate.isBefore(today)) {
            // Usage was not reset today (or never reset), meaning today's usage is effectively 0.
            // This relies on the backend/RPC to correctly report dailyMicrophoneUsageMs *for the current day*
            // or for the 'update_microphone_usage' RPC to reset it when it's the first usage of a new day.
            // If dailyMicrophoneUsageMs is a cumulative value that only resets when last_usage_reset_date changes,
            // and the RPC `update_microphone_usage` also updates `last_usage_reset_date` if it's a new day,
            // then this logic is sound.
            0 < FREE_TIER_DAILY_LIMIT_MS // True, as usage for today is 0.
        } else {
            // lastResetDate is somehow in the future, treat as an error or not allowed.
            false
        }
    }

    /**
     * Gets the remaining free tier microphone usage for today in milliseconds.
     * Returns Long.MAX_VALUE if subscribed or if there's an issue with usage data.
     */
    fun getRemainingDailyFreeUsageMs(): Long {
        val data = _subscriptionData.value ?: return 0 // If no data, assume no time left

        if (data.isSubscriptionActive()) {
            return Long.MAX_VALUE // Effectively infinite for subscribed users
        }

        val today = LocalDate.now(ZoneOffset.UTC)
        val lastResetDate = data.lastUsageResetEpochMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }

        return if (lastResetDate != null && lastResetDate.isEqual(today)) {
            (FREE_TIER_DAILY_LIMIT_MS - data.dailyMicrophoneUsageMs).coerceAtLeast(0L)
        } else if (lastResetDate == null || lastResetDate.isBefore(today)) {
            FREE_TIER_DAILY_LIMIT_MS // Full quota for today
        } else {
            0L // Error case or future reset date
        }
    }

    // Call this after a successful login or when user context is established
    fun initializeUserSession() {
        // Clear any existing data that might be from a different user
        _subscriptionData.value = null
        lastFetchedTimeMillis = 0L
        // Fetch initial data for the new user session in background
        GlobalScope.launch(Dispatchers.IO) { // Use GlobalScope or a specific app lifecycle scope
            refreshSubscriptionData()
        }
    }

    // Call this on logout
    fun clearUserSession() {
        _subscriptionData.value = null
        lastFetchedTimeMillis = 0L
    }

    // For Java interop, providing a callback version
    fun canActivateMicrophoneJava(forceRefresh: Boolean = false, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch { // Launch a coroutine to call the suspend function
            val result = canActivateMicrophone(forceRefresh)
            // Ensure callback is invoked on the main thread if it interacts with UI,
            // but here it's just for service logic, so current thread (IO) might be fine,
            // or switch to Main if there are strict threading requirements for the callback's actions.
            // For now, assume service logic can handle callback from IO thread.
            callback(result)
        }
    }

    fun getSubscriptionStatusForDisplay(): String {
        val data = _subscriptionData.value
        return when {
            data == null -> "Status: Unknown"
            data.isSubscriptionActive() -> {
                val expiry = data.subscriptionExpiryEpochMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                } ?: "Lifetime"
                "Status: Subscribed (Expires: $expiry)"
            }
            else -> "Status: Free Tier"
        }
    }

    fun getRemainingUsageForDisplay(): String {
        val data = _subscriptionData.value
        return when {
            data == null -> "Usage: Unknown"
            data.isSubscriptionActive() -> "Usage: Unlimited"
            else -> {
                val remainingMs = getRemainingDailyFreeUsageMs()
                if (remainingMs <= 0) {
                    "Usage: Daily limit reached"
                } else {
                    val minutes = remainingMs / (1000 * 60)
                    "Usage: Approx. $minutes min remaining today"
                }
            }
        }
    }
}
