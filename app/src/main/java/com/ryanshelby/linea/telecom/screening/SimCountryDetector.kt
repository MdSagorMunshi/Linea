package com.ryanshelby.linea.telecom.screening

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SimCountryInfo(
    val iso: String,           // e.g. "US", "BD", "GB"
    val countryName: String,   // e.g. "United States", "Bangladesh", "United Kingdom"
    val dialCode: String,      // e.g. "+1", "+880", "+44"
    val flagEmoji: String,     // e.g. "🇺🇸", "🇧🇩", "🇬🇧"
    val source: String         // e.g. "SIM Card (UICC)", "Mobile Network", "System Locale"
)

@Singleton
class SimCountryDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager
) {
    /**
     * Resolves the user's current country using on-device Telephony / SIM card hardware.
     * Strictly 100% offline: zero location sensor (GPS/fused), zero IP address lookups, zero network traffic.
     */
    fun getSimCountryIso(): String {
        return detectSimCountryIso(context, telephonyManager)
    }

    fun getSimCountryInfo(): SimCountryInfo {
        val iso = getSimCountryIso()
        val country = OfflineCallerIdEngine.getCountryInfoByIso(iso)
        val source = detectCountrySource(context, telephonyManager)
        return SimCountryInfo(
            iso = iso,
            countryName = country?.name ?: "Unknown Country",
            dialCode = country?.dialCode ?: "+1",
            flagEmoji = country?.flag ?: "🌐",
            source = source
        )
    }

    companion object {
        @Volatile
        var overrideCountryIso: String? = null

        @Volatile
        private var cachedCountryIso: String? = null

        val currentCountryIso: String
            get() = overrideCountryIso
                ?: cachedCountryIso
                ?: Locale.getDefault().country?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.length == 2 }
                ?: "US"

        fun setOverride(iso: String?) {
            overrideCountryIso = iso?.trim()?.uppercase(Locale.ROOT)
        }

        fun clearCache() {
            cachedCountryIso = null
        }

        /**
         * Detects the country ISO code in priority order:
         * 1. Manual test/user override if set
         * 2. TelephonyManager SIM provider ISO (telephonyManager.simCountryIso)
         * 3. SubscriptionManager active SIM cards (multi-SIM support)
         * 4. TelephonyManager cell network MCC country (telephonyManager.networkCountryIso)
         * 5. Device system locale
         *
         * Requires ZERO dangerous permissions (no ACCESS_FINE_LOCATION, no ACCESS_COARSE_LOCATION).
         */
        fun detectSimCountryIso(
            context: Context?,
            telephonyManager: TelephonyManager? = null
        ): String {
            overrideCountryIso?.let { return it }
            cachedCountryIso?.let { return it }

            val tm = telephonyManager ?: (context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)

            // 1. Primary SIM Country ISO from TelephonyManager
            val simIso = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!simIso.isNullOrBlank() && simIso.length == 2) {
                cachedCountryIso = simIso
                return simIso
            }

            // 2. Multi-SIM SubscriptionManager inspection
            if (context != null) {
                try {
                    val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                    val subList = sm?.activeSubscriptionInfoList
                    if (!subList.isNullOrEmpty()) {
                        for (sub in subList) {
                            val subIso = sub.countryIso?.trim()?.uppercase(Locale.ROOT)
                            if (!subIso.isNullOrBlank() && subIso.length == 2) {
                                cachedCountryIso = subIso
                                return subIso
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignored: fallback to network/locale
                }
            }

            // 3. Registered Mobile Network Country ISO (from cell tower MCC)
            val networkIso = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT)
            if (!networkIso.isNullOrBlank() && networkIso.length == 2) {
                cachedCountryIso = networkIso
                return networkIso
            }

            // 4. Fallback to device system locale
            val localeIso = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
                context.resources.configuration.locales[0]?.country?.trim()?.uppercase(Locale.ROOT)
            } else {
                Locale.getDefault().country?.trim()?.uppercase(Locale.ROOT)
            }
            if (!localeIso.isNullOrBlank() && localeIso.length == 2) {
                cachedCountryIso = localeIso
                return localeIso
            }

            val defaultIso = "US"
            cachedCountryIso = defaultIso
            return defaultIso
        }

        fun detectCountrySource(
            context: Context?,
            telephonyManager: TelephonyManager? = null
        ): String {
            if (overrideCountryIso != null) return "Manual Override"

            val tm = telephonyManager ?: (context?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
            val simIso = tm?.simCountryIso?.trim()
            if (!simIso.isNullOrBlank() && simIso.length == 2) return "SIM Card (UICC)"

            if (context != null) {
                try {
                    val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                    val subList = sm?.activeSubscriptionInfoList
                    if (!subList.isNullOrEmpty() && subList.any { !it.countryIso.isNullOrBlank() }) {
                        return "SIM Subscription"
                    }
                } catch (_: Exception) {}
            }

            val networkIso = tm?.networkCountryIso?.trim()
            if (!networkIso.isNullOrBlank() && networkIso.length == 2) return "Cellular Network (MCC)"

            return "System Locale"
        }
    }
}
