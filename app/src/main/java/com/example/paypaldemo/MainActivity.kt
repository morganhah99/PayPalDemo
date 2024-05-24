package com.example.paypaldemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient

class MainActivity : AppCompatActivity() {

    private lateinit var coreConfig: CoreConfig
    private lateinit var payPalNativeClient: PayPalNativeCheckoutClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coreConfig = CoreConfig(
            clientId = "AdbEgpEPd-S4Np4w4jgpGo_ZWUwrtzAxtlIBPBQKhiTtI1CrER-OFhDASjkGe_DAedeLMAme9I_fFwBA",
            environment = Environment.SANDBOX
        )

        payPalNativeClient = PayPalNativeCheckoutClient(
            application = this.application,
            coreConfig = coreConfig,
            returnUrl = "com.example.paypaldemo://paypalpay"
        )


    }
}