package com.hifnawy.circulardurationviewdemo.view

import android.graphics.Rect
import android.view.View

/**
 * Utility functions for working with [View].
 *
 * This class provides functions that make it easier to work with [View].
 * It provides a set of extension functions that can be called on any [View] instance.
 *
 * @author AbdAlMoniem AlHifnawy
 */
object ViewExtensionFunctions {

    /**
     * Sets a listener to be called when the size of the view changes.
     *
     * This extension function adds an OnLayoutChangeListener to the view which executes the provided
     * callback whenever the view's size changes. The callback provides the new and old dimensions of
     * the view, allowing developers to respond to size changes.
     *
     * @param callback A function to be invoked when the view's size changes. It receives the view itself,
     * the new width and height, and the old width and height as parameters.
     */
    inline fun View.onSizeChange(crossinline callback: (view: View, newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) -> Unit) =
            addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                val rect = Rect(left, top, right, bottom)
                val oldRect = Rect(oldLeft, oldTop, oldRight, oldBottom)

                if (rect.width() == oldRect.width() && rect.height() == oldRect.height()) return@addOnLayoutChangeListener

                callback(view, rect.width(), rect.height(), oldRect.width(), oldRect.height())
            }
}