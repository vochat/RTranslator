package nie.translator.rtranslator.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.ProductDetails
import nie.translator.rtranslator.R

class ProductDetailsAdapter(
    private var productDetailsList: List<ProductDetails>,
    private val onPurchaseClick: (ProductDetails) -> Unit
) : RecyclerView.Adapter<ProductDetailsAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_product, parent, false) // Create this layout file
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val productDetails = productDetailsList[position]
        holder.bind(productDetails)
    }

    override fun getItemCount(): Int = productDetailsList.size

    fun updateData(newData: List<ProductDetails>) {
        productDetailsList = newData
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.productTitle)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.productDescription)
        private val priceTextView: TextView = itemView.findViewById(R.id.productPrice)
        private val purchaseButton: Button = itemView.findViewById(R.id.buttonPurchase)

        fun bind(productDetails: ProductDetails) {
            titleTextView.text = productDetails.title
            descriptionTextView.text = productDetails.description

            // Subscriptions usually have one base plan.
            // Pricing display might need to be more sophisticated for multiple offers/plans.
            val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
            priceTextView.text = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "N/A"

            purchaseButton.setOnClickListener {
                onPurchaseClick(productDetails)
            }
        }
    }
}
