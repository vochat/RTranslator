package nie.translator.rtranslator.tools

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PurchaseValidationStatus {
    PENDING, SUCCESS, FAILED, ERROR, UNKNOWN
}

class BillingManager(
    private val context: Context,
    private val supabaseManager: SupabaseManager, // For calling validation Edge Function
    private val defaultScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        const val TAG = "BillingManager"
        // Define your subscription product IDs here (must match Play Console)
        val SUBSCRIPTION_PRODUCT_IDS = listOf("rtranslator_monthly_sub", "rtranslator_yearly_sub") // Example IDs
    }

    private lateinit var billingClient: BillingClient

    private val _productDetailsList = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetailsList = _productDetailsList.asStateFlow()

    private val _userPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val userPurchases = _userPurchases.asStateFlow()

    private val _isBillingClientConnected = MutableStateFlow(false)
    val isBillingClientConnected = _isBillingClientConnected.asStateFlow()

    // For purchase flow status updates to UI
    private val _purchaseFlowStatus = MutableStateFlow<PurchaseValidationStatus>(PurchaseValidationStatus.UNKNOWN)
    val purchaseFlowStatus = _purchaseFlowStatus.asStateFlow()


    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _purchaseFlowStatus.value = PurchaseValidationStatus.PENDING // Validation pending
                        handlePurchase(purchase)
                    } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                        Log.d(TAG, "Purchase is pending: ${purchase.orderId}")
                        // Inform UI about pending state if necessary
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase flow.")
                _purchaseFlowStatus.value = PurchaseValidationStatus.FAILED
            }
            else -> {
                Log.e(TAG, "Purchase failed with error code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                _purchaseFlowStatus.value = PurchaseValidationStatus.ERROR
            }
        }
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases() // Required for subscriptions and one-time products
            .build()

        connectToBillingService()
    }

    fun connectToBillingService() {
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "BillingClient setup successful")
                        _isBillingClientConnected.value = true
                        querySubscriptionProductDetails() // Query for products on successful connection
                        queryUserSubscriptions() // Query for existing user subscriptions
                    } else {
                        Log.e(TAG, "BillingClient setup failed: ${billingResult.debugMessage}")
                        _isBillingClientConnected.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "BillingClient disconnected. Retrying...")
                    _isBillingClientConnected.value = false
                    // Implement retry logic with backoff if desired, or prompt user.
                    // connectToBillingService() // Simple retry, can lead to loop
                }
            })
        }
    }

    fun querySubscriptionProductDetails(productIds: List<String> = SUBSCRIPTION_PRODUCT_IDS) {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient not ready to query products.")
            return
        }

        val productList = productIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, fetchedProductDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetailsList.value = fetchedProductDetailsList
                Log.d(TAG, "Fetched ProductDetails: $fetchedProductDetailsList")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient not ready, cannot launch purchase flow.")
            _purchaseFlowStatus.value = PurchaseValidationStatus.ERROR
            // Optionally, try to reconnect or inform user
            return
        }

        // Assuming it's a subscription, it should have offer tokens.
        // For a simple subscription, we might take the first one.
        // For more complex scenarios (e.g., multiple base plans, trial offers), select the correct token.
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer token found for product: ${productDetails.productId}")
            _purchaseFlowStatus.value = PurchaseValidationStatus.ERROR
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        _purchaseFlowStatus.value = PurchaseValidationStatus.UNKNOWN // Reset before new flow

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult.debugMessage}")
            _purchaseFlowStatus.value = PurchaseValidationStatus.ERROR
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Server-side validation is recommended for subscriptions.
            defaultScope.launch {
                val isValid = validatePurchaseWithServer(purchase.purchaseToken, purchase.products.first())
                if (isValid) {
                    // Acknowledge the purchase if not already acknowledged and if consumable (not typical for subs)
                    // or if auto-acknowledge is false. Subscriptions are typically auto-acknowledged or handled by server.
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase.purchaseToken)
                    }
                    _purchaseFlowStatus.value = PurchaseValidationStatus.SUCCESS
                    queryUserSubscriptions() // Refresh user's current subscriptions
                } else {
                     _purchaseFlowStatus.value = PurchaseValidationStatus.FAILED // Validation failed
                    // Optionally, handle refund or revocation if server validation fails.
                }
            }
        }
    }

    private suspend fun validatePurchaseWithServer(purchaseToken: String, productId: String): Boolean {
        Log.d(TAG, "Validating purchase with server: token=$purchaseToken, product=$productId")
        // This will call a Supabase Edge Function
        // The Edge Function is responsible for Google Play Developer API interaction.
        return try {
            // Placeholder: Replace with actual SupabaseManager call to the Edge Function
            // val validationResult = supabaseManager.invokeValidateGooglePlayPurchaseFunction(purchaseToken, productId)
            // return validationResult.isSuccess 
            // For now, assume validation passes if RPC doesn't fail, or returns true
            // This RPC needs to be created in SupabaseManager
            supabaseManager.invokeValidateGooglePlayPurchaseRpc(purchaseToken, productId)

        } catch (e: Exception) {
            Log.e(TAG, "Server-side validation failed", e)
            false
        }
    }


    private fun acknowledgePurchase(purchaseToken: String) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully.")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryUserSubscriptions() {
        if (!billingClient.isReady) {
            Log.e(TAG, "BillingClient not ready to query subscriptions.")
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _userPurchases.value = purchases
                Log.d(TAG, "User has ${purchases.size} active subscriptions.")
                purchases.forEach { purchase ->
                    Log.d(TAG, "  Product: ${purchase.products}, Token: ${purchase.purchaseToken}, State: ${purchase.purchaseState}, Ack: ${purchase.isAcknowledged}")
                     // If a purchase is found and purchased but not acknowledged, handle it.
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        // This might happen if validation/acknowledgment was interrupted.
                        // Re-trigger validation and acknowledgment.
                         handlePurchase(purchase)
                    }
                }
            } else {
                Log.e(TAG, "Failed to query user subscriptions: ${billingResult.debugMessage}")
            }
        }
    }

    fun onDestroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
