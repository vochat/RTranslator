import { serve } from "https://deno.land/std@0.170.0/http/server.ts";
import { google } from "googleapis"; // Ensure this is in import_map.json
import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

console.log("Edge Function 'validate-google-play-purchase' booting up.");

// These should be set as environment variables in your Supabase project settings
const GOOGLE_PLAY_SERVICE_ACCOUNT_JSON = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
const APP_PACKAGE_NAME = Deno.env.get("APP_PACKAGE_NAME") || "nie.translator.rtranslator"; // Default if not set

interface PurchasePayload {
  purchase_token: string;
  product_id: string;
}

async function updateSubscriptionStatus(
  supabaseAdmin: SupabaseClient,
  userId: string,
  productId: string,
  expiryTimeMillis: string, // String from Google API
  purchaseToken: string
) {
  const expiryDate = new Date(parseInt(expiryTimeMillis, 10)).toISOString();
  console.log(`Updating subscription for user ${userId}: productId=${productId}, expiryDate=${expiryDate}`);

  const { data, error } = await supabaseAdmin
    .from("user_profiles") // Or your dedicated subscriptions table
    .update({
      is_subscribed: true,
      subscription_expiry_date: expiryDate,
      // Storing these can be useful for audit or re-validation
      google_product_id: productId,
      google_purchase_token: purchaseToken,
      last_validated_time: new Date().toISOString(),
    })
    .eq("user_id", userId);

  if (error) {
    console.error("Error updating user profile:", error);
    throw new Error(`Failed to update subscription status: ${error.message}`);
  }
  console.log("User profile updated successfully:", data);
  return data;
}

serve(async (req: Request) => {
  if (!GOOGLE_PLAY_SERVICE_ACCOUNT_JSON) {
    console.error("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON env var not set.");
    return new Response("Server configuration error: Missing Google Play credentials.", { status: 500 });
  }
  if (!APP_PACKAGE_NAME) {
    console.error("APP_PACKAGE_NAME env var not set.");
    return new Response("Server configuration error: Missing App Package Name.", { status: 500 });
  }

  let payload: PurchasePayload;
  try {
    payload = await req.json();
  } catch (e) {
    return new Response("Invalid JSON payload", { status: 400 });
  }

  const { purchase_token, product_id } = payload;
  if (!purchase_token || !product_id) {
    return new Response("Missing purchase_token or product_id", { status: 400 });
  }

  // Get user ID from Supabase auth token (JWT)
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return new Response("Missing Authorization header", { status: 401 });
  }
  const supabaseClient = createClient( // For fetching user from token
    Deno.env.get("SUPABASE_URL") ?? '',
    Deno.env.get("SUPABASE_ANON_KEY") ?? '',
    { global: { headers: { Authorization: authHeader } } }
  );
  const { data: { user }, error: userError } = await supabaseClient.auth.getUser();

  if (userError || !user) {
    console.error("Auth error:", userError);
    return new Response("Authentication failed", { status: 401 });
  }

  const userId = user.id;
  console.log(`Processing purchase for user: ${userId}, product: ${product_id}, token: ${purchase_token.substring(0,20)}...`);

  try {
    const jwtClient = new google.auth.JWT({
      email: JSON.parse(GOOGLE_PLAY_SERVICE_ACCOUNT_JSON).client_email,
      key: JSON.parse(GOOGLE_PLAY_SERVICE_ACCOUNT_JSON).private_key,
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });

    const androidPublisher = google.androidpublisher({
      version: "v3",
      auth: jwtClient,
    });

    // Validate the subscription purchase
    const subGetResponse = await androidPublisher.purchases.subscriptions.get({
      packageName: APP_PACKAGE_NAME,
      subscriptionId: product_id,
      token: purchase_token,
    });

    if (subGetResponse.status !== 200 || !subGetResponse.data) {
      console.error("Google API error or no data:", subGetResponse);
      return new Response("Failed to validate purchase with Google", { status: 400 });
    }

    const subscription = subGetResponse.data;
    console.log("Google API subscription data:", subscription);

    // Check if the purchase is valid (e.g., paymentState, expiryTimeMillis)
    // paymentState: 0=Pending, 1=Received, 2=Free trial, (deprecated: 3=Deferred)
    if (subscription.paymentState === undefined || subscription.paymentState === null || subscription.paymentState === 0) {
         // For subscriptions, an active subscription should have expiryTimeMillis.
        // If paymentState is 0 (pending), we might not update our DB yet, or mark as pending.
        // For simplicity, we only proceed if it's clearly active.
        // AcknowledgeState: 0=Yet to be acknowledged, 1=Acknowledged.
        // If server-side acknowledgment is used, this might be 0 initially.
        // The client already tries to acknowledge, but server can also do it.
        // For subscriptions, ensure autoRenewing is true or expiryTimeMillis is in the future.
      if (!subscription.expiryTimeMillis || parseInt(subscription.expiryTimeMillis, 10) < Date.now()) {
        console.warn("Subscription is expired or payment pending/failed.");
        // Consider if this should be an error or just non-action.
        // If a purchase token for an expired sub is sent, it's usually not an error, but means no active sub.
        return new Response("Subscription is not active or payment is pending.", { status: 200, body: JSON.stringify({ success: false, message: "Subscription not active."}) });
      }
    }

    // Use Supabase Admin client to update user_profiles table (bypass RLS)
    // Ensure SUPABASE_SERVICE_ROLE_KEY is set in env for admin client.
     const supabaseAdminClient = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
      );

    await updateSubscriptionStatus(
      supabaseAdminClient,
      userId,
      product_id,
      subscription.expiryTimeMillis!, // expiryTimeMillis should be present for active subs
      purchase_token
    );

    return new Response(JSON.stringify({ success: true, message: "Subscription validated and status updated." }), {
      headers: { "Content-Type": "application/json" },
      status: 200,
    });

  } catch (error) {
    console.error("Error in validation process:", error);
    let errorMessage = "Internal server error during purchase validation.";
    if (error.errors && error.errors.length > 0 && error.errors[0].message) {
        errorMessage = `Google API Error: ${error.errors[0].message}`;
    } else if (error.message) {
        errorMessage = error.message;
    }
    return new Response(JSON.stringify({ success: false, error: errorMessage }), {
      headers: { "Content-Type": "application/json" },
      status: 500,
    });
  }
});
