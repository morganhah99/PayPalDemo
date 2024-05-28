package com.example.paypaldemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.paypaldemo.databinding.ActivityMainBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutRequest
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var coreConfig: CoreConfig
    private lateinit var payPalNativeClient: PayPalNativeCheckoutClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        analytics = Firebase.analytics
        setContentView(binding.root)

        val successMessage = binding.tvSuccessMessage
        val productImage = binding.ivProductImage
        val productPrice = binding.tvProductPrice
        val paypalButton = binding.paypalButton

        coreConfig = CoreConfig(
            clientId = "AdbEgpEPd-S4Np4w4jgpGo_ZWUwrtzAxtlIBPBQKhiTtI1CrER-OFhDASjkGe_DAedeLMAme9I_fFwBA",
            environment = Environment.SANDBOX
        )

        payPalNativeClient = PayPalNativeCheckoutClient(
            application = this.application,
            coreConfig = coreConfig,
            returnUrl = "com.example.paypaldemo://paypalpay"
        )

        payPalNativeClient.listener = object : PayPalNativeCheckoutListener {
            override fun onPayPalCheckoutStart() {
                Log.i("PAYPAL", "PayPal checkout started")
            }

            override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
                Log.i("PAYPAL", "PayPal checkout success: $result")
                analytics.logEvent("paypal_checkout_finish", null)
                successMessage.visibility = View.VISIBLE
                productImage.visibility = View.GONE
                paypalButton.visibility = View.GONE
                productPrice.visibility = View.GONE

                lifecycleScope.launch {
                    val accessToken = getAccessToken()
                    if (accessToken != null) {
                        result.orderId?.let { captureOrder(it, accessToken) }
                    } else {
                        Log.i("PAYPAL", "Failed to obtain access token")
                    }
                }
            }

            override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
                Log.i("PAYPAL", "PayPal checkout error: $error")
            }

            override fun onPayPalCheckoutCanceled() {
                Log.i("PAYPAL", "PayPal checkout canceled")
                analytics.logEvent("paypal_checkout_cancelled", null)
            }
        }

        val payPalButton: PayPalButton = binding.paypalButton

        payPalButton.setOnClickListener {
            lifecycleScope.launch {
                startPayPalCheckout()
            }
        }
    }

    private suspend fun startPayPalCheckout() {
        val orderId = getOrderId()
        val request = orderId?.let { PayPalNativeCheckoutRequest(it) }
        if (request != null) {
            payPalNativeClient.startCheckout(request)
        }
        analytics.logEvent("paypal_checkout_start", null)
    }

    private suspend fun getOrderId(): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val accessToken = getAccessToken() ?: return@withContext null

        val authHeader = "Bearer $accessToken"
        val requestBody = """
            {
                "intent": "CAPTURE",
                "purchase_units": [{
                    "amount": {
                        "currency_code": "USD", 
                        "value": "10.00"
                    }
                }]
            }
        """.trimIndent().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("https://api-m.sandbox.paypal.com/v2/checkout/orders")
            .post(requestBody)
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val jsonObject = JSONObject(responseBody)
                    return@withContext jsonObject.getString("id")
                }
            } else {
                Log.i("PAYPAL", "Error: ${response.body?.string()}")
            }
        } catch (e: IOException) {
            Log.i("PAYPAL", "Failure: ${e.message}")
        }
        return@withContext null
    }

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val secret =
            "EORUvftMgIfuRQx5BmrGFP0lRYOsdengTR0sEV4cKXRYhDN5HJzSh9g0x_WZtc89IGtiGc_PvhqngLh_"
        val auth = Credentials.basic(coreConfig.clientId, secret)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api-m.sandbox.paypal.com/v1/oauth2/token")
            .post("grant_type=client_credentials".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .addHeader("Authorization", auth)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val json = responseBody?.let { JSONObject(it) }
                return@withContext json?.getString("access_token")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private suspend fun captureOrder(orderId: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api-m.sandbox.paypal.com/v2/checkout/orders/$orderId/capture")
                .post("".toRequestBody(null))
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.i("PAYPAL", "Order captured successfully")
                } else {
                    Log.i("PAYPAL", "Failed to capture order: ${response.message}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
}