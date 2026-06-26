package com.paywise.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.paywise.app.domain.model.SubscriptionPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREMIUM_MONTHLY_SKU = "paywise_premium_monthly"
        private const val PREMIUM_YEARLY_SKU = "paywise_premium_yearly"
        private const val PREMIUM_FREE_TRIAL_SKU = "paywise_premium_trial"
    }

    private var billingClient: BillingClient? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _purchaseState = MutableStateFlow(PurchaseState.IDLE)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _entitlements = MutableStateFlow<List<String>>(emptyList())
    val entitlements: StateFlow<List<String>> = _entitlements.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    enum class PurchaseState {
        IDLE, LOADING, PURCHASED, FAILED, RESTORED, NOT_PURCHASED, CANCELLED
    }

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    _purchaseState.value = PurchaseState.CANCELLED
                } else {
                    _purchaseState.value = PurchaseState.FAILED
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    querySkuDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }

    private fun querySkuDetails() {
        val skuList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY_SKU)
                .setProductType("subs")
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY_SKU)
                .setProductType("subs")
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_FREE_TRIAL_SKU)
                .setProductType("subs")
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(skuList).build()

        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            productDetailsList?.forEach { details ->
                details.productId?.let { id -> productDetailsMap[id] = details }
            }
        }
    }

    private fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType("subs").build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val activeSubs = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _isPremium.value = activeSubs.isNotEmpty()
        _entitlements.value = activeSubs.flatMap { it.products }
    }

    fun launchBillingFlow(activity: Activity, sku: String = PREMIUM_MONTHLY_SKU) {
        val productDetails = productDetailsMap[sku] ?: run {
            _purchaseState.value = PurchaseState.FAILED
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()

        _purchaseState.value = PurchaseState.LOADING
        billingClient?.launchBillingFlow(activity, params)
    }

    fun restorePurchases() {
        queryPurchases()
        scope.launch {
            delay(500)
            if (_isPremium.value) {
                _purchaseState.value = PurchaseState.RESTORED
            } else {
                _purchaseState.value = PurchaseState.NOT_PURCHASED
            }
        }
    }

    fun checkSubscriptionStatus(onResult: (SubscriptionPlan) -> Unit) {
        onResult(if (_isPremium.value) SubscriptionPlan.PREMIUM else SubscriptionPlan.FREE)
    }

    fun isFeatureAvailable(feature: String): Boolean {
        return _isPremium.value || _entitlements.value.contains(feature)
    }

    fun destroy() {
        billingClient?.endConnection()
        scope.cancel()
    }
}
