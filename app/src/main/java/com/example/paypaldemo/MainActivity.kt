package com.example.paypaldemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.paypaldemo.databinding.ActivityMainBinding
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutRequest
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var coreConfig: CoreConfig
    private lateinit var payPalNativeClient: PayPalNativeCheckoutClient
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val successMessage = binding.tvSuccessMessage
        val productImage = binding.ivProductImage
        val productPrice = binding.tvProductPrice

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
                getAccessToken { accessToken ->
                    if (accessToken != null) {
                        result.orderId?.let { captureOrder(it, accessToken) }
                        runOnUiThread{
                            successMessage.visibility = View.VISIBLE
                            productImage.visibility = View.GONE
                            productPrice.visibility = View.GONE
                        }
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
            }
        }

        val payPalButton: PayPalButton = findViewById(R.id.paypal_button)
        payPalButton.setOnClickListener {
            startPayPalCheckout()
        }
    }

    private fun startPayPalCheckout() {
        getOrderId { orderid ->
            val request = orderid?.let { PayPalNativeCheckoutRequest(it) }
            if (request != null) {
                payPalNativeClient.startCheckout(request)
            }
        }
    }

    private fun getOrderId(callback: (String?) -> Unit) {
        val client = OkHttpClient()
        getAccessToken { accessToken ->
            accessToken?.let { token ->
                val authHeader = "Bearer $token"
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

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.i("PAYPAL", "Failure: ${e.message}")
                        callback(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            response.body?.string()?.let { responseBody ->
                                val jsonObject = JSONObject(responseBody)
                                val orderId = jsonObject.getString("id")
                                callback(orderId)
                            }
                        } else {
                            Log.i("PAYPAL", "Error: ${response.body?.string()}")
                            callback(null)
                        }
                    }
                })
            } ?: run {
                Log.i("PAYPAL", "Access token is null")
                callback(null)
            }
        }
    }

    private fun getAccessToken(callback: (String?) -> Unit) {
        val secret = "EORUvftMgIfuRQx5BmrGFP0lRYOsdengTR0sEV4cKXRYhDN5HJzSh9g0x_WZtc89IGtiGc_PvhqngLh_"
        val auth = Credentials.basic(coreConfig.clientId, secret)

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api-m.sandbox.paypal.com/v1/oauth2/token")
            .post("grant_type=client_credentials".toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .addHeader("Authorization", auth)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = responseBody?.let { JSONObject(it) }
                    val accessToken = json?.getString("access_token")
                    callback(accessToken)
                } else {
                    callback(null)
                }
            }
        })
    }

    private fun captureOrder(orderId: String, accessToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api-m.sandbox.paypal.com/v2/checkout/orders/$orderId/capture")
            .post("".toRequestBody(null))
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("PAYPAL", "Order captured successfully")
                } else {
                    Log.i("PAYPAL", "Failed to capture order: ${response.message}")
                }
            }
        })
    }
}