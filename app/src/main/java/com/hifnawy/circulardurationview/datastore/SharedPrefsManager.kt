package com.hifnawy.circulardurationview.datastore

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.hifnawy.circulardurationview.Application
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.SharedPrefsKeys.ENABLE_MATERIAL_YOU
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.SharedPrefsKeys.ENABLE_PICTURE_IN_PICTURE
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.SharedPrefsKeys.THEME
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.Theme.DARK
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.Theme.LIGHT
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager.Theme.SYSTEM_DEFAULT

/**
 * Manages shared preferences for the application, providing convenient methods to access and modify them.
 *
 * This class encapsulates the shared preferences logic, allowing clients to easily retrieve and update preferences
 * related to permissions, themes, and other settings. It also notifies registered observers of changes.
 *
 * @param application The application instance used to access shared preferences.
 *
 * @constructor Creates an instance of [SharedPrefsManager] with the provided [Application].
 *
 * @author AbdAlMoniem AlHifnawy
 *
 * @see Application
 */
class SharedPrefsManager(private val application: Application) {

    /**
     * An enumeration of the shared preferences keys used by the application.
     *
     * These keys are used to store and retrieve values from the shared preferences of the application.
     *
     * @property THEME A [Theme] value indicating the current theme of the application.
     * @property ENABLE_MATERIAL_YOU A [Boolean] value indicating whether Material You design elements should be enabled.
     *
     * @author AbdAlMoniem AlHifnawy
     */
    private enum class SharedPrefsKeys {

        /**
         * A [Theme] value indicating the current theme of the application.
         */
        THEME,

        /**
         * A [Boolean] value indicating whether Material You design elements should be enabled.
         */
        ENABLE_MATERIAL_YOU,

        /**
         * A [Boolean] value indicating whether the application should be enabled in picture-in-picture mode.
         */
        ENABLE_PICTURE_IN_PICTURE,
    }

    /**
     * An enumeration of the theme options that are available to the user.
     *
     * Themes control the overall visual appearance of the application. The theme can be set to one of the following values:
     * @property SYSTEM_DEFAULT Follows the device's system theme.
     * @property LIGHT A light theme.
     * @property DARK A dark theme.
     *
     * @property mode [Int] The value of the theme, which is used to set the theme for the application.
     */
    enum class Theme(var mode: Int) {

        /**
         * A theme that follows the device's system theme.
         *
         * The application will follow the device's system theme setting, which can be either light or dark.
         *
         * @see AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
         */
        SYSTEM_DEFAULT(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),

        /**
         * A light theme.
         *
         * The application will use a light theme, which is suitable for daytime use.
         *
         * @see AppCompatDelegate.MODE_NIGHT_NO
         */
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),

        /**
         * A dark theme.
         *
         * The application will use a dark theme, which is suitable for nighttime use.
         *
         * @see AppCompatDelegate.MODE_NIGHT_YES
         */
        DARK(AppCompatDelegate.MODE_NIGHT_YES)
    }

    private val sharedPreferences by lazy { application.getSharedPreferences(application.packageName, Context.MODE_PRIVATE) }

    /**
     * Retrieves or sets the current theme of the application.
     *
     * This property is used to get or update the theme preference stored in shared preferences.
     * It allows the application to persist the user's theme choice across sessions.
     *
     * @return [Theme] The current theme set in the application.
     *
     * @see SharedPrefsManager.Theme
     */
    var theme: Theme
        get() = Theme.valueOf(sharedPreferences.getString(THEME.name, SYSTEM_DEFAULT.name) ?: SYSTEM_DEFAULT.name)
        set(value) = sharedPreferences.edit().putString(THEME.name, value.name).apply()

    /**
     * Retrieves or sets whether the "Material You" feature is enabled.
     *
     * This property is used to determine if the "Material You" dynamic theming feature is enabled in the application.
     * It allows the application to apply or remove dynamic theming based on the user's preference stored in shared preferences.
     *
     * @return [Boolean] `true` if the "Material You" feature is enabled, `false` otherwise.
     */
    var isMaterialYouEnabled: Boolean
        get() = sharedPreferences.getBoolean(ENABLE_MATERIAL_YOU.name, false)
        set(value) = sharedPreferences.edit().putBoolean(ENABLE_MATERIAL_YOU.name, value).apply()

    /**
     * Retrieves or sets whether picture-in-picture mode is enabled.
     *
     * This property is used to get or update the preference of whether picture-in-picture mode should be enabled.
     * It allows the application to persist the user's preference across sessions.
     *
     * @return [Boolean] `true` if picture-in-picture mode is enabled, `false` otherwise.
     */
    var isPictureInPictureEnabled: Boolean
        get() = sharedPreferences.getBoolean(ENABLE_PICTURE_IN_PICTURE.name, false)
        set(value) {
            sharedPreferences.edit().putBoolean(ENABLE_PICTURE_IN_PICTURE.name, value).apply()
            notifySharedPrefsObservers { observer -> observer.onIsPictureInPictureEnabledUpdated(value) }
        }

    /**
     * Notifies all registered observers of a change in the shared preferences.
     *
     * The callback provided will be called on each registered observer. The callback should take a single parameter, which is
     * the observer to be notified. The callback should call the appropriate method on the observer to notify it of the change
     * in shared preferences.
     *
     * @param notifyCallback [(observer: SharedPrefsObserver) -> Unit][notifyCallback] the callback to be called on each registered observer.
     */
    private fun notifySharedPrefsObservers(notifyCallback: (observer: SharedPrefsObserver) -> Unit) =
            application.sharedPrefsObservers.forEach(notifyCallback)
}

/**
 * A listener interface for observing changes to shared preferences.
 *
 * This interface should be implemented by classes that wish to be notified when specific shared preferences change.
 * Implementing classes can register themselves as observers and respond to changes in preferences by overriding the
 * methods provided in this interface. Each method corresponds to a specific preference and is called whenever the
 * associated preference changes.
 *
 * @see Observer
 * @see SharedPrefsManager
 */
interface SharedPrefsObserver : Observer {

    /**
     * Called when the "Picture in Picture Enabled" preference changes.
     *
     * @param isPictureInPictureEnabled [Boolean] `true` if the "Picture in Picture" feature is enabled, `false` otherwise.
     */
    fun onIsPictureInPictureEnabledUpdated(isPictureInPictureEnabled: Boolean) = Unit
}