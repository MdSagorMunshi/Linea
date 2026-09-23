package com.ryanshelby.linea.nfc

import android.content.Context
import android.content.SharedPreferences
import com.ryanshelby.linea.data.preferences.SocialPlatform
import com.ryanshelby.linea.data.preferences.UserProfile
import com.ryanshelby.linea.qr_code.QrCodeEngine

/**
 * Fast synchronous storage for LiNEA Beam personal profile, Always Share mode,
 * and pending contacts received while the app was closed.
 *
 * Essential for Android HostApduService (HCE) which runs on binder threads and cannot
 * suspend on asynchronous DataStore coroutines during time-critical NFC bumps.
 */
object NfcProfileStorage {
    private const val PREFS_NAME = "linea_nfc_profile_store"

    private const val KEY_ALWAYS_SHARE = "nfc_always_share_mode"
    private const val KEY_NAME = "profile_name"
    private const val KEY_NUMBER = "profile_number"
    private const val KEY_EMAIL = "profile_email"
    private const val KEY_PHOTO_BASE64 = "profile_photo_base64"
    private const val KEY_PHOTO_URI = "profile_photo_uri"
    private const val KEY_SOCIAL1_PLATFORM = "profile_social1_platform"
    private const val KEY_SOCIAL1_HANDLE = "profile_social1_handle"
    private const val KEY_SOCIAL2_PLATFORM = "profile_social2_platform"
    private const val KEY_SOCIAL2_HANDLE = "profile_social2_handle"

    private const val KEY_CACHED_URI = "profile_cached_uri"
    private const val KEY_PENDING_BEAMED_PAYLOAD = "pending_beamed_contact_uri"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save user profile and Always Share mode synchronously.
     */
    fun saveProfile(context: Context, profile: UserProfile, alwaysShare: Boolean? = null) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_NUMBER, profile.number)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_SOCIAL1_PLATFORM, profile.social1Platform.name)
            .putString(KEY_SOCIAL1_HANDLE, profile.social1Handle)
            .putString(KEY_SOCIAL2_PLATFORM, profile.social2Platform.name)
            .putString(KEY_SOCIAL2_HANDLE, profile.social2Handle)

        if (profile.photoBase64 != null) {
            editor.putString(KEY_PHOTO_BASE64, profile.photoBase64)
        } else {
            editor.remove(KEY_PHOTO_BASE64)
        }

        if (profile.photoUri != null) {
            editor.putString(KEY_PHOTO_URI, profile.photoUri)
        } else {
            editor.remove(KEY_PHOTO_URI)
        }

        if (alwaysShare != null) {
            editor.putBoolean(KEY_ALWAYS_SHARE, alwaysShare)
        }

        val payload = profile.toContactPayload()
        if (payload != null) {
            val uri = QrCodeEngine.serializeToUri(payload)
            editor.putString(KEY_CACHED_URI, uri)
        } else {
            editor.remove(KEY_CACHED_URI)
        }

        // Use commit() to guarantee data is immediately flushed to disk synchronously
        editor.commit()
    }

    fun isAlwaysShareEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALWAYS_SHARE, false)
    }

    fun setAlwaysShareEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALWAYS_SHARE, enabled).commit()
    }

    /**
     * Synchronously load user profile.
     */
    fun loadProfile(context: Context): UserProfile {
        val prefs = getPrefs(context)
        val s1PlatformStr = prefs.getString(KEY_SOCIAL1_PLATFORM, null) ?: SocialPlatform.NONE.name
        val s2PlatformStr = prefs.getString(KEY_SOCIAL2_PLATFORM, null) ?: SocialPlatform.NONE.name
        return UserProfile(
            name = prefs.getString(KEY_NAME, "") ?: "",
            number = prefs.getString(KEY_NUMBER, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            photoBase64 = prefs.getString(KEY_PHOTO_BASE64, null),
            photoUri = prefs.getString(KEY_PHOTO_URI, null),
            social1Platform = try { SocialPlatform.valueOf(s1PlatformStr) } catch (_: Exception) { SocialPlatform.NONE },
            social1Handle = prefs.getString(KEY_SOCIAL1_HANDLE, "") ?: "",
            social2Platform = try { SocialPlatform.valueOf(s2PlatformStr) } catch (_: Exception) { SocialPlatform.NONE },
            social2Handle = prefs.getString(KEY_SOCIAL2_HANDLE, "") ?: ""
        )
    }

    /**
     * Returns pre-cached serialized URI or null if no profile set.
     */
    fun getCachedUri(context: Context): String? {
        val cached = getPrefs(context).getString(KEY_CACHED_URI, null)
        if (!cached.isNullOrBlank()) return cached
        val payload = loadProfile(context).toContactPayload() ?: return null
        val uri = QrCodeEngine.serializeToUri(payload)
        getPrefs(context).edit().putString(KEY_CACHED_URI, uri).commit()
        return uri
    }

    fun getEffectivePayload(context: Context): QrCodeEngine.ContactPayload? {
        return loadProfile(context).toContactPayload()
    }

    /**
     * Stash incoming beamed contact received while app is in background or closed.
     */
    fun savePendingBeamedContact(context: Context, payload: QrCodeEngine.ContactPayload) {
        val uri = QrCodeEngine.serializeToUri(payload)
        getPrefs(context).edit().putString(KEY_PENDING_BEAMED_PAYLOAD, uri).commit()
    }

    /**
     * Read and clear any pending beamed contact received while app was closed.
     */
    fun consumePendingBeamedContact(context: Context): QrCodeEngine.ContactPayload? {
        val prefs = getPrefs(context)
        val rawUri = prefs.getString(KEY_PENDING_BEAMED_PAYLOAD, null) ?: return null
        prefs.edit().remove(KEY_PENDING_BEAMED_PAYLOAD).commit()
        return QrCodeEngine.parseLineaScannedText(rawUri)
    }
}
