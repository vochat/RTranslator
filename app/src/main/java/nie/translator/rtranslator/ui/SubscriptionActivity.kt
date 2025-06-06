package nie.translator.rtranslator.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import nie.translator.rtranslator.Global
import nie.translator.rtranslator.R // Assume R.layout.activity_subscription and view IDs exist
import nie.translator.rtranslator.tools.BillingManager
import nie.translator.rtranslator.tools.PurchaseValidationStatus
import nie.translator.rtranslator.tools.SubscriptionManager

class SubscriptionActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SubscriptionActivity"
    }

    private lateinit var billingManager: BillingManager
    private lateinit var subscriptionManager: SubscriptionManager

    private lateinit var recyclerViewProducts: RecyclerView
    private lateinit var productDetailsAdapter: ProductDetailsAdapter
    private lateinit var textViewCurrentStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription) // Create this layout file

        val global = applicationContext as Global
        // Assuming SupabaseManager is already initialized in Global
        billingManager = BillingManager(this, global.supabaseManager, lifecycleScope)
        subscriptionManager = global.subscriptionManager // Get from Global

        textViewCurrentStatus = findViewById(R.id.textViewCurrentSubscriptionStatus)
        progressBar = findViewById(R.id.progressBarSubscription)
        recyclerViewProducts = findViewById(R.id.recyclerViewSubscriptionProducts)

        setupRecyclerView()
        observeBillingState()
        observeSubscriptionState()

        billingManager.connectToBillingService() // Connect and query products
    }

    private fun setupRecyclerView() {
        productDetailsAdapter = ProductDetailsAdapter(emptyList()) { productDetails ->
            // Handle purchase click
            billingManager.launchPurchaseFlow(this, productDetails)
        }
        recyclerViewProducts.layoutManager = LinearLayoutManager(this)
        recyclerViewProducts.adapter = productDetailsAdapter
    }

    private fun observeBillingState() {
        lifecycleScope.launch {
            billingManager.isBillingClientConnected.collectLatest { isConnected ->
                if (isConnected) {
                    Log.d(TAG, "Billing client connected. Querying products.")
                    // Products are usually queried automatically on connect by BillingManager
                } else {
                    Log.d(TAG, "Billing client not connected.")
                }
            }
        }

        lifecycleScope.launch {
            billingManager.productDetailsList.collectLatest { products ->
                Log.d(TAG, "Products updated: ${products.size}")
                productDetailsAdapter.updateData(products)
                progressBar.visibility = if (products.isNotEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            billingManager.purchaseFlowStatus.collectLatest { status ->
                when (status) {
                    PurchaseValidationStatus.SUCCESS -> {
                        Toast.makeText(this@SubscriptionActivity, "Purchase successful!", Toast.LENGTH_LONG).show()
                        // Refresh subscription status from our backend
                        subscriptionManager.refreshSubscriptionData()
                    }
                    PurchaseValidationStatus.FAILED -> {
                        Toast.makeText(this@SubscriptionActivity, "Purchase failed or cancelled.", Toast.LENGTH_SHORT).show()
                    }
                    PurchaseValidationStatus.ERROR -> {
                        Toast.makeText(this@SubscriptionActivity, "Error during purchase.", Toast.LENGTH_SHORT).show()
                    }
                    PurchaseValidationStatus.PENDING -> {
                         Toast.makeText(this@SubscriptionActivity, "Purchase validation pending...", Toast.LENGTH_SHORT).show()
                    }
                    else -> {} // Unknown or initial state
                }
            }
        }
    }

    private fun observeSubscriptionState() {
        lifecycleScope.launch {
            subscriptionManager.subscriptionData.collectLatest { subData ->
                if (subData != null) {
                    val statusText = subscriptionManager.getSubscriptionStatusForDisplay() +
                                     "\n" + subscriptionManager.getRemainingUsageForDisplay()
                    textViewCurrentStatus.text = statusText
                } else {
                    textViewCurrentStatus.text = "Loading subscription status..."
                    // Optionally trigger a refresh if null for a while
                    subscriptionManager.refreshSubscriptionData()
                }
            }
        }
    }

    override fun onDestroy() {
        billingManager.onDestroy() // Important to end connection
        super.onDestroy()
    }
}
