package com.hifnawy.circulardurationview

import android.app.Application
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager
import com.hifnawy.circulardurationview.datastore.SharedPrefsObserver

/**
 * The main application class for the Caffeinate app.
 *
 * This class extends [Application] and is responsible for initializing global state,
 * setting up shared preferences, and managing application-wide resources.
 *
 * @property sharedPrefsObservers [List] A list of [SharedPrefsObserver] objects that are
 * notified when shared preferences change.
 *
 * @see Application
 * @see SharedPrefsManager
 *
 * @author AbdAlMoniem AlHifnawy
 */
class Application : Application() {


    /**
     * A list of observers that are notified whenever a shared preference changes.
     *
     * This list is used to store all observers that are registered to receive notifications whenever a shared preference changes.
     *
     * When a shared preference changes, the [SharedPrefsManager] notifies all observers in this list of the change.
     *
     * @see SharedPrefsManager.notifySharedPrefsObservers
     */
    var sharedPrefsObservers = mutableListOf<SharedPrefsObserver>()
}