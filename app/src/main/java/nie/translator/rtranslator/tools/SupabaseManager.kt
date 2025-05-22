package nie.translator.rtranslator.tools

import io.supabase.gotrue.GoTrue
import io.supabase.gotrue.OtpType
import io.supabase.gotrue.Session
import io.supabase.gotrue.UserResponse
import io.supabase.gotrue.ext.FlowType
import io.supabase.gotrue.handleDeeplinks
import io.supabase.gotrue.providers.Github
import io.supabase.gotrue.providers.Google
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context // Added for SessionManager
import io.supabase.postgrest.PostgrestClient
import io.supabase.postgrest.rpc.RpcRequestBuilder
import io.supabase.postgrest.query.PostgrestRequestBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


// Data class for User Profile
@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("subscription_expiry_date") val subscriptionExpiryDate: String? = null,
    @SerialName("daily_microphone_usage_ms") val dailyMicrophoneUsageMs: Long = 0L,
    @SerialName("last_usage_reset_date") val lastUsageResetDate: String? = null
)

class SupabaseManager(private val context: Context) {

    private val supabaseUrl = "https://ewfelixdytuzfufwepgka.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV3ZmVsaXhkeXR1emZ1ZndlcGdrYSIsInJvbGUiOiJhbm9uIiwiaWF0IjoxNzE0OTM4NTM4LCJleHAiOjIwMzA1MTQ1Mzh9.3pL2P5aLg3232q968J77ARsZ2rrG5yC52LG2Vf2A_2s"

    private lateinit var sessionManager: SessionManager
    private val authClient: GoTrue // Renamed client to authClient for clarity
    private val postgrestClient: PostgrestClient

    init {
        authClient = GoTrue(
            url = supabaseUrl,
            headers = mapOf(
                // "Authorization" to "Bearer $supabaseAnonKey", // GoTrue handles its own auth
                "apikey" to supabaseAnonKey
            ),
            autoLoadFromStorage = false
        )

        postgrestClient = PostgrestClient(
            uri = "$supabaseUrl/rest/v1", // Standard PostgREST endpoint
            headers = {
                // Dynamically provide headers, especially the Authorization token
                val currentSession = authClient.currentSession()
                val authHeader = currentSession?.accessToken?.let { "Bearer $it" } ?: "Bearer $supabaseAnonKey"
                mapOf(
                    "apikey" to supabaseAnonKey,
                    "Authorization" to authHeader
                )
            }
        )

        sessionManager = SessionManager(context)
        val loadedSession = sessionManager.loadSession()
        if (loadedSession != null && loadedSession.accessToken.isNotBlank() && loadedSession.refreshToken.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                 try {
                    authClient.recoverSession(loadedSession.accessToken, loadedSession.refreshToken!!)
                 } catch (e: Exception) {
                    sessionManager.clearSession()
                    e.printStackTrace()
                 }
            }
        }
    }

    /* // Commenting out email/password signUp
    // Function to sign up a new user
    suspend fun signUp(email: String, password: String): UserResponse? {
        return try {
            val response = authClient.signUp(email = email, password = password)
            response?.session?.let { session ->
                sessionManager.saveSession(session)
            }
            authClient.currentSession()?.let { current ->
                 if(current.user != null && current.accessToken.isNotBlank()){
                    sessionManager.saveSession(current)
                 }
            }
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    */

    /* // Commenting out email/password login
    // Function to log in an existing user
    suspend fun login(email: String, password: String): Session? {
        return try {
            val session = authClient.login(email = email, password = password)
            session?.let {
                sessionManager.saveSession(it)
            }
            session
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    */

    // Function to log out the current user
    suspend fun logout() {
        try {
            authClient.logout()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sessionManager.clearSession()
            // Notify Global about user logout
            (context.applicationContext as? Global)?.onUserLogout()
        }
    }

    // Function to get the current user
    fun getCurrentUser(): UserResponse? {
        return authClient.currentUser()
    }

    // Function to get the current session
    fun getCurrentSession(): Session? {
        return authClient.currentSession()
    }

    // Example of how to handle deep links for OAuth (optional)
    fun handleDeepLink(intent: android.content.Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authClient.handleDeeplinks(intent, flowType = FlowType.PKCE)
                authClient.currentSession()?.let { session ->
                    if(session.user != null && session.accessToken.isNotBlank()){
                        sessionManager.saveSession(session)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Example for OAuth with Google (optional)
    suspend fun signInWithGoogle(activityContext: Context, redirectUrl: String = "io.supabase.rtranslator://callback") {
        try {
            authClient.loginWithProvider(
                provider = Google,
                context = activityContext,
                redirectUrl = redirectUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Example for OAuth with GitHub (optional)
    suspend fun signInWithGitHub(activityContext: Context, redirectUrl: String = "io.supabase.rtranslator://callback") {
        try {
            authClient.loginWithProvider(
                provider = Github,
                context = activityContext,
                redirectUrl = redirectUrl
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* // Commenting out sendPasswordResetEmail
    // Function to send a password reset email
    suspend fun sendPasswordResetEmail(email: String) {
        try {
            authClient.sendPasswordReset(email = email)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    */

    /* // Commenting out updateUserPassword
    // Function to update user password (requires user to be logged in)
    suspend fun updateUserPassword(newPassword: String): UserResponse? {
        return try {
            authClient.updateUser(password = newPassword)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    */

    // Function to call Supabase RPC for updating microphone usage
    suspend fun invokeUpdateMicrophoneUsageRpc(usageDurationMs: Long): Boolean {
        // Ensure user is logged in, otherwise RPC might fail or be insecure
        if (getCurrentSession() == null || getCurrentUser() == null) {
            println("User not authenticated, cannot update microphone usage.")
            return false
        }
        return try {
            val parameters = buildJsonObject {
                put("usage_duration_ms", usageDurationMs)
            }
            // Assuming the RPC function is named 'update_microphone_usage'
            // and it's callable with POST. The specific client.rpc method might vary.
            // This is a common way to call RPCs with postgrest-kt
            postgrestClient.rpc(function = "update_microphone_usage", parameters = parameters)
            // Check for success. RPC calls might not return content on success (204 No Content)
            // or might return specific data. For simplicity, assume any non-error response is success.
            // A more robust check would inspect the response status code (e.g., response.status == HttpStatusCode.OK or No Content)
            // This part depends on how postgrest-kt exposes response details from RPC calls.
            // For now, if it doesn't throw, we assume success.
            true 
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Function to fetch user profile data
    suspend fun fetchUserProfile(): UserProfile? {
        val currentUser = getCurrentUser()?.user
        if (currentUser == null) {
            println("User not authenticated, cannot fetch profile.")
            return null
        }
        return try {
            // Query 'user_profiles' table where user_id matches the current user's ID
            // Select all columns for now, or specify them: "user_id, is_subscribed, subscription_expiry_date, daily_microphone_usage_ms, last_usage_reset_date"
            val response = postgrestClient.from("user_profiles")
                .select {
                    filter("user_id", PostgrestRequestBuilder.Operator.EQ, currentUser.id)
                }
                .executeAndGetSingle<UserProfile>() // Expects a single row or throws if not found/multiple
            
            response
        } catch (e: Exception) {
            // This can happen if the profile doesn't exist yet for the user, or network error, etc.
            // Postgrest typically throws specific exceptions for not found (e.g., if executeAndGetSingle fails)
            // For simplicity, catching general Exception here.
            e.printStackTrace()
            // Optionally, create a default/empty profile locally if one doesn't exist on backend
            // if (e is io.supabase.postgrest.exceptions.NotFoundException) { return UserProfile(userId = currentUser.id) }
            null
        }
    }

    // Function to call Supabase Edge Function for validating Google Play purchase
    suspend fun invokeValidateGooglePlayPurchaseRpc(purchaseToken: String, productId: String): Boolean {
        if (getCurrentSession() == null || getCurrentUser() == null) {
            println("User not authenticated, cannot validate purchase.")
            return false
        }
        return try {
            val parameters = buildJsonObject {
                put("purchase_token", purchaseToken)
                put("product_id", productId)
                // The Edge Function might also need/get user_id from the session token implicitly
            }
            // Ensure the function name matches your Edge Function deployment
            postgrestClient.rpc(function = "validate-google-play-purchase", parameters = parameters)
            // Assuming the Edge Function returns a simple success/failure or handles errors by throwing.
            // If it returns a specific JSON payload like { "success": true }, you'd need to deserialize it.
            // For now, if RPC doesn't throw, assume it indicated success to the backend.
            // A more robust implementation would check the HTTP status or response body from the Edge Function.
            true 
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error calling validate-google-play-purchase Edge Function: ${e.message}")
            false
        }
    }
}
