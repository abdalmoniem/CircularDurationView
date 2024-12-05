package com.hifnawy.circulardurationview

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R.attr.colorPrimary
import com.google.android.material.R.attr.colorPrimaryContainer
import com.google.android.material.R.attr.colorSecondary
import com.google.android.material.R.attr.colorSecondaryContainer
import com.google.android.material.R.attr.colorTertiary
import com.google.android.material.R.attr.colorTertiaryContainer
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * A custom view that displays a duration in a circular format.
 *
 * - The duration can be in seconds, minutes or hours
 * - The progress indicators can be animated or not
 * - The view is divided into three parts: hours, minutes and seconds
 * - For each part, a progress indicator is displayed to show the progress
 * of that part of the duration
 *
 * @author AbdAlMoniem AlHifnawy
 * @constructor Creates a new instance of the view.
 * @param context The context in which the view is used.
 * @param attrs The attributes that are set in the layout file.
 * @param defStyleAttr The default style of the attributes.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CircularDurationView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Converts the given [Int] value from pixels to dp (density-independent pixels).
     *
     * This is a convenience extension function that multiplies the given [Int] value by the density
     * of the device's display and rounds it to the nearest whole number.
     *
     * @return the given value converted from pixels to dp (density-independent pixels).
     */
    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

    /**
     * The attach state change listener for the view.
     *
     * @return [AttachStateChangeListener] The attach state change listener for the view.
     */
    private val mAttachStateChangeListener by lazy { AttachStateChangeListener() }

    /**
     * The progress indicator for the hours.
     *
     * @return [CircularProgressIndicator] The progress indicator for the hours.
     */
    private var mHoursIndicator: CircularProgressIndicator

    /**
     * The progress indicator for the minutes.
     *
     * @return [CircularProgressIndicator] The progress indicator for the minutes.
     */
    private var mMinutesIndicator: CircularProgressIndicator

    /**
     * The progress indicator for the seconds.
     *
     * @return [CircularProgressIndicator] The progress indicator for the seconds.
     */
    private var mSecondsIndicator: CircularProgressIndicator

    /**
     * The paint used to draw the text.
     *
     * @return [Paint] The paint used to draw the text.
     */
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Gets or sets the text to be displayed.
     *
     * @return [String] The text to be displayed.
     */
    private var mText = ""

    /**
     * Gets or sets the color of the text.
     *
     * @return [Int] The color of the text.
     */
    private var mTextColor = MaterialColors.getColor(this, colorPrimary, Color.WHITE)

    /**
     * Gets or sets the style of the text.
     *
     * @return [Int] The style of the text.
     */
    private var mTextStyle = Typeface.NORMAL

    /**
     * Gets or sets the alignment of the text.
     *
     * @return [Int] The alignment of the text.
     */
    private var mTextAlign = Paint.Align.CENTER

    /**
     * Gets or sets the padding of the text.
     *
     * @return [Int] The padding of the text.
     */
    private var mTextPadding = 0.dp

    /**
     * Gets or sets the font family of the text.
     *
     * @return [Int] The font family of the text.
     */
    private var mTextFontFamily = 0

    /**
     * Gets or sets whether the text should be displayed.
     *
     * @return [Boolean] Whether the text should be displayed.
     */
    private var mShowSubText = false

    /**
     * Gets or sets the padding of the sub text.
     *
     * @return [Int] The padding of the sub text.
     */
    private var mSubTextPadding = 5.dp

    /**
     * Gets or sets the typeface of the text.
     *
     * @return [Typeface] The typeface of the text.
     */
    private var mTypeFace = Typeface.DEFAULT

    /**
     * Gets or sets the size of the progress indicator.
     *
     * @return [Int] The size of the progress indicator.
     */
    private var mIndicatorSize = -1

    /**
     * Gets or sets the size of the gap between indicators.
     *
     * @return [Int] The size of the gap between indicators.
     */
    private var mIndicatorsGapSize = 5.dp

    /**
     * Gets or sets the color of the indicators.
     *
     * @return [Int] The color of the indicators.
     */
    private var mIndicatorsColor = MaterialColors.getColor(this, colorPrimary, Color.WHITE)

    /**
     * Gets or sets the color of the indicators.
     *
     * @return [Int] The color of the indicators.
     */
    private var mIndicatorsTrackColor = MaterialColors.getColor(this, colorPrimaryContainer, Color.WHITE)

    /**
     * Gets or sets the thickness of the indicators.
     *
     * @return [Int] The thickness of the indicators.
     */
    private var mIndicatorsTrackThickness = 15.dp

    /**
     * Gets or sets the corner radius of the indicators.
     *
     * @return [Int] The corner radius of the indicators.
     */
    private var mIndicatorsTrackCornerRadius = 10.dp

    /**
     * Gets or sets the gap size of the indicators.
     *
     * @return [Int] The gap size of the indicators.
     */
    private var mIndicatorsTrackGapSize = 10.dp

    /**
     * Gets or sets whether the indicators should be animated.
     *
     * @return [Boolean] Whether the indicators should be animated.
     */
    private var mIsAnimated = true

    /**
     * Gets or sets the delay between each animation.
     *
     * @return [Int] The delay between each animation.
     */
    private var mStaggeredInfiniteAnimationDelay = 50

    /**
     * The lifecycle scope of the view.
     *
     * @return [LifecycleCoroutineScope] The lifecycle scope of the view.
     */
    private var mLifecycleScope: LifecycleCoroutineScope? = null

    /**
     * The job that is used to stagger the animation of the indicators when the [progress] is [Duration.INFINITE].
     *
     * This job is used to create a staggered animation effect when the indicators are updated. The job
     * is launched when the indicators are updated and it is responsible for updating the progress of each
     * indicator in sequence with a delay between each update.
     *
     * @return [Job] The job that is used to stagger the animation of the indicators.
     *
     * @see isInfinite
     */
    private var mStaggerAnimationJob: Job? = null

    /**
     * Gets or sets the maximum value of the hours indicator.
     *
     * @return [Int] The maximum value of the hours indicator.
     */
    private var mHoursIndicatorMax = 24

    /**
     * Gets or sets the progress of the hours indicator.
     *
     * @return [Int] The progress of the hours indicator.
     */
    private var mHoursIndicatorProgress = 0

    /**
     * Gets or sets the color of the hours indicator.
     *
     * @return [Int] The color of the hours indicator.
     */
    private var mHoursIndicatorColor = MaterialColors.getColor(this, colorPrimary, Color.WHITE)

    /**
     * Gets or sets the color of the hours indicator.
     *
     * @return [Int] The color of the hours indicator.
     */
    private var mHoursIndicatorTrackColor = MaterialColors.getColor(this, colorPrimaryContainer, Color.WHITE)

    /**
     * Gets or sets the thickness of the hours indicator.
     *
     * @return [Int] The thickness of the hours indicator.
     */
    private var mHoursIndicatorTrackThickness = -1

    /**
     * Gets or sets the corner radius of the hours indicator.
     *
     * @return [Int] The corner radius of the hours indicator.
     */
    private var mHoursIndicatorTrackCornerRadius = -1

    /**
     * Gets or sets the gap size of the hours indicator.
     *
     * @return [Int] The gap size of the hours indicator.
     */
    private var mHoursIndicatorTrackGapSize = -1

    /**
     * Gets or sets the progress of the minutes indicator.
     *
     * @return [Int] The progress of the minutes indicator.
     */
    private var mMinutesIndicatorProgress = 0

    /**
     * Gets or sets the color of the minutes indicator.
     *
     * @return [Int] The color of the minutes indicator.
     */
    private var mMinutesIndicatorColor = MaterialColors.getColor(this, colorSecondary, Color.WHITE)

    /**
     * Gets or sets the color of the minutes indicator.
     *
     * @return [Int] The color of the minutes indicator.
     */
    private var mMinutesIndicatorTrackColor = MaterialColors.getColor(this, colorSecondaryContainer, Color.WHITE)

    /**
     * Gets or sets the thickness of the minutes indicator.
     *
     * @return [Int] The thickness of the minutes indicator.
     */
    private var mMinutesIndicatorTrackThickness = -1

    /**
     * Gets or sets the corner radius of the minutes indicator.
     *
     * @return [Int] The corner radius of the minutes indicator.
     */
    private var mMinutesIndicatorTrackCornerRadius = -1

    /**
     * Gets or sets the track gap size for the minutes indicator.
     *
     * @return [Int] The track gap size for the minutes indicator.
     */
    private var mMinutesIndicatorTrackGapSize = -1

    /**
     * Gets or sets the progress of the seconds indicator.
     *
     * @return [Int] The progress of the seconds indicator.
     */
    private var mSecondsIndicatorProgress = 0

    /**
     * Gets or sets the color of the seconds indicator.
     *
     * @return [Int] The color of the seconds indicator.
     */
    private var mSecondsIndicatorColor = MaterialColors.getColor(this, colorTertiary, Color.WHITE)

    /**
     * Gets or sets the color of the seconds indicator.
     *
     * @return [Int] The color of the seconds indicator.
     */
    private var mSecondsIndicatorTrackColor = MaterialColors.getColor(this, colorTertiaryContainer, Color.WHITE)

    /**
     * Gets or sets the thickness of the seconds indicator.
     *
     * @return [Int] The thickness of the seconds indicator.
     */
    private var mSecondsIndicatorTrackThickness = -1

    /**
     * Gets or sets the corner radius of the seconds indicator.
     *
     * @return [Int] The corner radius of the seconds indicator.
     */
    private var mSecondsIndicatorTrackCornerRadius = -1

    /**
     * Gets or sets the track gap size for the seconds indicator.
     *
     * @return [Int] The track gap size for the seconds indicator.
     */
    private var mSecondsIndicatorTrackGapSize = -1

    /**
     * Gets or sets the duration of the progress.
     *
     * @return [Duration] The duration of the progress.
     */
    private var mProgress: Duration = 0.seconds

    /**
     * Gets or sets the text displayed on the view.
     *
     * This property defines the text that will be displayed on the view. It is used to
     * configure the text that will be displayed when the view is drawn.
     *
     * @return [String] The text displayed on the view.
     */
    var text
        get() = mText
        set(value) {
            mText = value

            invalidate()
        }

    /**
     * Gets or sets the color of the text.
     *
     * This property defines the color of the text. It is used to configure
     * the color of the text when displayed.
     *
     * @return [Int] The color of the text.
     */
    var textColor
        get() = mTextColor
        set(value) {
            mTextColor = value

            invalidate()
        }

    /**
     * Gets or sets the style of the text.
     *
     * This property defines the style of the text. It is used to configure
     * the style of the text when displayed.
     *
     * @return [Int] The style of the text.
     */
    var textStyle
        get() = mTextStyle
        set(value) {
            mTextStyle = value

            invalidate()
        }

    /**
     * Gets or sets the alignment of the text.
     *
     * This property defines the alignment of the text. It is used to configure
     * the alignment of the text when displayed.
     *
     * @return [Int] The alignment of the text.
     */
    var textAlign
        get() = mTextAlign
        set(value) {
            mTextAlign = value

            invalidate()
        }

    /**
     * Gets or sets the padding of the text.
     *
     * This property defines the padding of the text. It is used to configure
     * the padding of the text when displayed.
     *
     * @return [Int] The padding of the text.
     */
    var textPadding
        get() = mTextPadding
        set(value) {
            mTextPadding = value

            invalidate()
        }

    /**
     * Gets or sets the font family of the text.
     *
     * This property defines the font family of the text. It is used to configure
     * the font of the text when displayed.
     *
     * @return [Int] The font family of the text.
     */
    var textFontFamily
        get() = mTextFontFamily
        set(value) {
            mTextFontFamily = value

            invalidate()
        }

    /**
     * Gets or sets whether to show the sub text.
     *
     * This property defines whether to show the sub text. It is used to configure
     * whether to show the sub text when displayed.
     *
     * @return [Boolean] Whether to show the sub text.
     */
    var showSubText
        get() = mShowSubText
        set(value) {
            mShowSubText = value

            invalidate()
        }

    /**
     * Gets or sets the padding of the sub text.
     *
     * This property defines the padding of the sub text. It is used to configure
     * the padding of the sub text when displayed.
     *
     * @return [Int] The padding of the sub text.
     */
    var subTextPadding
        get() = mSubTextPadding
        set(value) {
            mSubTextPadding = value

            invalidate()
        }

    /**
     * Gets or sets the size of the progress indicator.
     *
     * This property defines the size of the progress indicator. It is used
     * to configure the size of the indicator when displayed.
     *
     * @return [Int] The size of the progress indicator.
     */
    var indicatorSize
        get() = mIndicatorSize
        set(value) {
            mIndicatorSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the size of the gap between indicators.
     *
     * This property defines the size of the gap between the indicators. It is used
     * to configure the spacing and layout of the indicators when displayed.
     *
     * @return [Int] The size of the gap between the indicators.
     */
    var indicatorsGapSize
        get() = mIndicatorsGapSize
        set(value) {
            mIndicatorsGapSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the indicators.
     *
     * The color of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is [colorPrimaryContainer].
     *
     * @return [Int] The color of the indicators.
     */
    var indicatorsColor
        get() = mIndicatorsColor
        set(value) {
            mIndicatorsColor = value
            mHoursIndicatorColor = value
            mMinutesIndicatorColor = value
            mSecondsIndicatorColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the indicators.
     *
     * The track color of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is [colorPrimaryContainer].
     *
     * @return [Int] The color of the indicators.
     */
    var indicatorsTrackColor
        get() = mIndicatorsTrackColor
        set(value) {
            mIndicatorsTrackColor = value
            mHoursIndicatorTrackColor = value
            mMinutesIndicatorTrackColor = value
            mSecondsIndicatorTrackColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the thickness of the indicators.
     *
     * The track thickness of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is 15dp.
     *
     * @return [Int] The thickness of the indicators.
     */
    var indicatorsTrackThickness
        get() = mIndicatorsTrackThickness
        set(value) {
            mIndicatorsTrackThickness = value
            mHoursIndicatorTrackThickness = value
            mMinutesIndicatorTrackThickness = value
            mSecondsIndicatorTrackThickness = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the corner radius of the indicators.
     *
     * The track corner radius of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is 10dp.
     *
     * @return [Int] The corner radius of the indicators.
     */
    var indicatorsTrackCornerRadius
        get() = mIndicatorsTrackCornerRadius
        set(value) {
            mIndicatorsTrackCornerRadius = value
            mHoursIndicatorTrackCornerRadius = value
            mMinutesIndicatorTrackCornerRadius = value
            mSecondsIndicatorTrackCornerRadius = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the gap size of the indicators.
     *
     * The track gap size of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is 10dp.
     *
     * @return [Int] The gap size of the indicators.
     */
    var indicatorsTrackGapSize
        get() = mIndicatorsTrackGapSize
        set(value) {
            mIndicatorsTrackGapSize = value
            mHoursIndicatorTrackGapSize = value
            mMinutesIndicatorTrackGapSize = value
            mSecondsIndicatorTrackGapSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the delay between the animation of the indicators.
     *
     * The delay between the animation of the indicators. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is 500ms.
     *
     * @return [Int] The delay between the animation of the indicators.
     */
    var staggeredInfiniteAnimationDelay
        get() = mStaggeredInfiniteAnimationDelay.milliseconds
        set(value) {
            mStaggeredInfiniteAnimationDelay = value.toInt(DurationUnit.MILLISECONDS)
        }

    /**
     * Gets or sets whether the indicators should be animated.
     *
     * Whether the indicators should be animated. This property is used to configure
     * the visual appearance of the indicators.
     *
     * The default value of this property is true.
     *
     * @return [Boolean] Whether the indicators should be animated.
     */
    var isAnimated
        get() = mIsAnimated
        set(value) {
            mIsAnimated = value
        }

    /**
     * Gets or sets the maximum value of the hours indicator.
     *
     * The maximum value of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is 24.
     *
     * @return [Int] The maximum value of the hours indicator.
     */
    var hoursIndicatorMax
        get() = mHoursIndicatorMax
        set(value) {
            mHoursIndicatorMax = value
            mHoursIndicator.max = value

            if (isInfinite) return

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the progress of the hours indicator.
     *
     * The progress of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The progress of the hours indicator.
     */
    var hoursIndicatorProgress
        get() = mHoursIndicatorProgress
        set(value) {
            mHoursIndicatorProgress = value

            val nanos = mProgress.toComponents { _, _, _, nanoseconds -> nanoseconds }
            mProgress = value.hours + minutesIndicatorProgress.minutes + secondsIndicatorProgress.seconds + nanos.nanoseconds

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the hours indicator.
     *
     * The color of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is [Color.WHITE].
     *
     * @return [Int] The color of the hours indicator.
     */
    var hoursIndicatorColor
        get() = mHoursIndicatorColor
        set(value) {
            mHoursIndicatorColor = value
        }

    /**
     * Gets or sets the color of the hours indicator.
     *
     * The track color of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is [Color.WHITE].
     *
     * @return [Int] The color of the hours indicator.
     */
    var hoursIndicatorTrackColor
        get() = mHoursIndicatorTrackColor
        set(value) {
            mHoursIndicatorTrackColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the thickness of the hours indicator.
     *
     * The track thickness of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The thickness of the hours indicator.
     */
    var hoursIndicatorTrackThickness
        get() = mHoursIndicatorTrackThickness
        set(value) {
            mHoursIndicatorTrackThickness = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the corner radius of the hours indicator.
     *
     * The track corner radius of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The corner radius of the hours indicator.
     */
    var hoursIndicatorTrackCornerRadius
        get() = mHoursIndicatorTrackCornerRadius
        set(value) {
            mHoursIndicatorTrackCornerRadius = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the gap size of the hours indicator.
     *
     * The track gap size of the hours indicator. This property is used to configure
     * the visual appearance of the hours indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The gap size of the hours indicator.
     */
    var hoursIndicatorTrackGapSize
        get() = mHoursIndicatorTrackGapSize
        set(value) {
            mHoursIndicatorTrackGapSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the minutes indicator.
     *
     * The color of the minutes indicator. This property is used to configure
     * the visual appearance of the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The color of the minutes indicator.
     */
    var minutesIndicatorColor
        get() = mMinutesIndicatorColor
        set(value) {
            mMinutesIndicatorColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the progress of the minutes indicator.
     *
     * The progress of the minutes indicator. This property is used to configure
     * the visual appearance of the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The progress of the minutes indicator.
     */
    var minutesIndicatorProgress
        get() = mMinutesIndicatorProgress
        set(value) {
            mMinutesIndicatorProgress = value

            val nanos = mProgress.toComponents { _, _, _, nanoseconds -> nanoseconds }
            mProgress = hoursIndicatorProgress.hours + value.minutes + secondsIndicatorProgress.seconds + nanos.nanoseconds

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the minutes indicator.
     *
     * The track color of the minutes indicator track. This property is used to
     * configure the visual appearance of the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The color of the minutes indicator.
     */
    var minutesIndicatorTrackColor
        get() = mMinutesIndicatorTrackColor
        set(value) {
            mMinutesIndicatorTrackColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the thickness of the minutes indicator.
     *
     * The track thickness of the minutes indicator. This property is used to
     * configure the visual appearance of the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The thickness of the minutes indicator.
     */
    var minutesIndicatorTrackThickness
        get() = mMinutesIndicatorTrackThickness
        set(value) {
            mMinutesIndicatorTrackThickness = value
        }

    /**
     * Gets or sets the corner radius of the minutes indicator.
     *
     * The track corner radius of the minutes indicator. This
     * property is used to configure the visual appearance of
     * the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The corner radius of the minutes indicator.
     */
    var minutesIndicatorTrackCornerRadius
        get() = mMinutesIndicatorTrackCornerRadius
        set(value) {
            mMinutesIndicatorTrackCornerRadius = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the gap size of the minutes indicator.
     *
     * The track gap size of the minutes indicator. This property is used to
     * configure the visual appearance of the minutes indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The gap size of the minutes indicator.
     */
    var minutesIndicatorTrackGapSize
        get() = mMinutesIndicatorTrackGapSize
        set(value) {
            mMinutesIndicatorTrackGapSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the progress of the seconds indicator.
     *
     * The progress of the seconds indicator. This property is used to
     * configure the visual appearance of the seconds indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The progress of the seconds indicator.
     */
    var secondsIndicatorProgress
        get() = mSecondsIndicatorProgress
        set(value) {
            mSecondsIndicatorProgress = value

            val nanos = mProgress.toComponents { _, _, _, nanoseconds -> nanoseconds }
            mProgress = hoursIndicatorProgress.hours + minutesIndicatorProgress.minutes + value.seconds + nanos.nanoseconds

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the seconds indicator.
     *
     * The color of the seconds indicator. This property is used to
     * configure the visual appearance of the seconds indicator.
     *
     * The default value of this property is the color {@link Color#WHITE}.
     *
     * @return [Int] The color of the seconds indicator.
     */
    var secondsIndicatorColor
        get() = mSecondsIndicatorColor
        set(value) {
            mSecondsIndicatorColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the color of the track of the seconds indicator.
     *
     * The color of the track of the seconds indicator. This property is used to
     * configure the visual appearance of the track of the seconds indicator.
     *
     * The default value of this property is the color {@link Color#WHITE}.
     *
     * @return [Int] The color of the track of the seconds indicator.
     */
    var secondsIndicatorTrackColor
        get() = mSecondsIndicatorTrackColor
        set(value) {
            mSecondsIndicatorTrackColor = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the thickness of the track of the seconds indicator.
     *
     * The thickness of the track of the seconds indicator. This property is used to
     * configure the visual appearance of the track of the seconds indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] The thickness of the track of the seconds indicator.
     */
    var secondsIndicatorTrackThickness
        get() = mSecondsIndicatorTrackThickness
        set(value) {
            mSecondsIndicatorTrackThickness = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the corner radius of the track of the seconds indicator.
     *
     * The corner radius of the track of the seconds indicator. This property is used to
     * configure the visual appearance of the track of the seconds indicator.
     *
     * The default value of this property is 0.
     *
     * @return [Int] the corner radius of the track of the seconds indicator.
     */
    var secondsIndicatorTrackCornerRadius
        get() = mSecondsIndicatorTrackCornerRadius
        set(value) {
            mSecondsIndicatorTrackCornerRadius = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the size of the gap between the track and the progress indicator.
     *
     * The gap is the space between the track and the progress indicator. The track is the outer
     * circle of the view, and the progress indicator is the inner circle that shows the progress.
     *
     * The default value is 0.
     *
     * @return [Int] the size of the gap between the track and the progress indicator.
     */
    var secondsIndicatorTrackGapSize
        get() = mSecondsIndicatorTrackGapSize
        set(value) {
            mSecondsIndicatorTrackGapSize = value

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets the progress of the view.
     *
     * The progress of the view is a [Duration] object that contains the hours, minutes, and seconds
     * values of the progress. The progress is used to update the progress of the indicators.
     *
     * If the progress is set to a [Duration] with a value of [Duration.INFINITE], the view will be
     * considered indeterminate. In this case, the indicators will be animated in an indeterminate
     * manner.
     *
     * @return [Duration] the progress of the view.
     */
    var progress
        get() = mProgress
        set(value) {
            isInfinite = value.isInfinite()
            if (isInfinite) return
            mProgress = value

            value.toComponents { hours, minutes, seconds, _ ->
                mHoursIndicator.isIndeterminate = false
                mMinutesIndicator.isIndeterminate = false
                mSecondsIndicator.isIndeterminate = false

                if (hours == 0L) hoursIndicatorMax = 24
                hoursIndicatorProgress = hours.toInt()
                minutesIndicatorProgress = minutes
                secondsIndicatorProgress = seconds
            }

            invalidate()
            requestLayout()
        }

    /**
     * Gets or sets whether the view is in an indeterminate state.
     *
     * If the view is in an indeterminate state, the indicators will be animated in an indeterminate
     * manner.
     *
     * @return [Boolean] `true` if the view is in an indeterminate state, `false` otherwise.
     */
    var isInfinite
        get() = mProgress.isInfinite()
        set(value) {
            if (value && isInfinite) return
            val nanos = mProgress.toComponents { _, _, _, nanoseconds -> nanoseconds }
            mProgress = when {
                value -> Duration.INFINITE
                else  -> hoursIndicatorProgress.hours + minutesIndicatorProgress.minutes + secondsIndicatorProgress.seconds + nanos.nanoseconds
            }

            mStaggerAnimationJob?.cancel()

            // stagger indeterminate animations
            if (value) mStaggerAnimationJob = mLifecycleScope?.launch {
                mHoursIndicator.isIndeterminate = true
                delay(mStaggeredInfiniteAnimationDelay.milliseconds)
                mMinutesIndicator.isIndeterminate = true
                delay(mStaggeredInfiniteAnimationDelay.milliseconds)
                mSecondsIndicator.isIndeterminate = true
            } else {
                mHoursIndicator.isIndeterminate = false
                mMinutesIndicator.isIndeterminate = false
                mSecondsIndicator.isIndeterminate = false
            }

            invalidate()
        }

    /**
     * Initializes the view.
     *
     * This method initializes the view by retrieving the styled attributes from the context
     * and applying them to the view.
     */
    init {
        addOnAttachStateChangeListener(mAttachStateChangeListener)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.CircularDurationView, defStyleAttr, 0)
        mText = attributes.getString(R.styleable.CircularDurationView_text) ?: ""
        mTextColor = attributes.getColor(R.styleable.CircularDurationView_textColor, textColor)
        mTextStyle = when (val txtStyle = attributes.getInt(R.styleable.CircularDurationView_textStyle, -1)) {
            -1   -> textStyle
            else -> txtStyle
        }
        mTextAlign = when (val txtAlign = attributes.getInt(R.styleable.CircularDurationView_textAlign, -1)) {
            -1   -> textAlign
            else -> Paint.Align.entries[txtAlign]
        }
        mTextPadding = attributes.getDimension(R.styleable.CircularDurationView_textPadding, textPadding.toFloat()).roundToInt()
        mTextFontFamily = attributes.getResourceId(R.styleable.CircularDurationView_textFontFamily, textFontFamily)
        when (mTextFontFamily) {
            0    -> Log.e(this@CircularDurationView::class.simpleName, "Font resource not set or invalid")
            else -> mTypeFace = ResourcesCompat.getFont(context, mTextFontFamily) ?: Typeface.DEFAULT
        }
        mTypeFace = Typeface.create(mTypeFace, mTextStyle)
        mShowSubText = attributes.getBoolean(R.styleable.CircularDurationView_showSubText, showSubText)
        mSubTextPadding = attributes.getDimension(R.styleable.CircularDurationView_subTextPadding, subTextPadding.toFloat()).roundToInt()

        mIndicatorSize = attributes.getDimension(R.styleable.CircularDurationView_indicatorSize, indicatorSize.toFloat()).roundToInt()
        mIndicatorsGapSize =
                attributes.getDimension(R.styleable.CircularDurationView_indicatorsGapSize, indicatorsGapSize.toFloat()).roundToInt()
        mIndicatorsColor = attributes.getColor(R.styleable.CircularDurationView_indicatorsColor, indicatorsColor)
        mIndicatorsTrackColor = attributes.getColor(R.styleable.CircularDurationView_indicatorsTrackColor, indicatorsTrackColor)
        mIndicatorsTrackThickness =
                attributes.getDimension(R.styleable.CircularDurationView_indicatorsTrackThickness, indicatorsTrackThickness.toFloat()).roundToInt()
        mIndicatorsTrackCornerRadius =
                attributes.getDimension(R.styleable.CircularDurationView_indicatorsTrackCornerRadius, indicatorsTrackCornerRadius.toFloat())
                    .roundToInt()
        mIndicatorsTrackGapSize =
                attributes.getDimension(R.styleable.CircularDurationView_indicatorsTrackGapSize, indicatorsTrackGapSize.toFloat()).roundToInt()
        mIsAnimated = attributes.getBoolean(R.styleable.CircularDurationView_animated, isAnimated)
        mStaggeredInfiniteAnimationDelay =
                attributes.getInt(
                        R.styleable.CircularDurationView_staggeredInfiniteAnimationDelay,
                        staggeredInfiniteAnimationDelay.toInt(DurationUnit.MILLISECONDS)
                )

        mHoursIndicatorMax = attributes.getInteger(R.styleable.CircularDurationView_hoursIndicatorMax, hoursIndicatorMax)
        mHoursIndicatorProgress = attributes.getInteger(R.styleable.CircularDurationView_hoursIndicatorProgress, hoursIndicatorProgress)
        mHoursIndicatorColor = attributes.getColor(R.styleable.CircularDurationView_hoursIndicatorColor, indicatorsColor)
        mHoursIndicatorTrackColor = attributes.getColor(R.styleable.CircularDurationView_hoursIndicatorTrackColor, indicatorsTrackColor)
        mHoursIndicatorTrackThickness =
                attributes.getDimension(R.styleable.CircularDurationView_hoursIndicatorTrackThickness, indicatorsTrackThickness.toFloat())
                    .roundToInt()
        mHoursIndicatorTrackCornerRadius =
                attributes.getDimension(R.styleable.CircularDurationView_hoursIndicatorTrackCornerRadius, indicatorsTrackCornerRadius.toFloat())
                    .roundToInt()
        mHoursIndicatorTrackGapSize =
                attributes.getDimension(R.styleable.CircularDurationView_hoursIndicatorTrackGapSize, indicatorsTrackGapSize.toFloat())
                    .roundToInt()

        mMinutesIndicatorProgress = attributes.getInteger(R.styleable.CircularDurationView_minutesIndicatorProgress, minutesIndicatorProgress)
        mMinutesIndicatorColor = attributes.getColor(R.styleable.CircularDurationView_minutesIndicatorColor, indicatorsColor)
        mMinutesIndicatorTrackColor = attributes.getColor(R.styleable.CircularDurationView_minutesIndicatorTrackColor, indicatorsTrackColor)
        mMinutesIndicatorTrackThickness =
                attributes.getDimension(R.styleable.CircularDurationView_minutesIndicatorTrackThickness, indicatorsTrackThickness.toFloat())
                    .roundToInt()
        mMinutesIndicatorTrackCornerRadius =
                attributes.getDimension(R.styleable.CircularDurationView_minutesIndicatorTrackCornerRadius, indicatorsTrackCornerRadius.toFloat())
                    .roundToInt()
        mMinutesIndicatorTrackGapSize =
                attributes.getDimension(R.styleable.CircularDurationView_minutesIndicatorTrackGapSize, indicatorsTrackGapSize.toFloat())
                    .roundToInt()

        mSecondsIndicatorProgress = attributes.getInteger(R.styleable.CircularDurationView_secondsIndicatorProgress, secondsIndicatorProgress)
        mSecondsIndicatorColor = attributes.getColor(R.styleable.CircularDurationView_secondsIndicatorColor, indicatorsColor)
        mSecondsIndicatorTrackColor = attributes.getColor(R.styleable.CircularDurationView_secondsIndicatorTrackColor, indicatorsTrackColor)
        mSecondsIndicatorTrackThickness =
                attributes.getDimension(R.styleable.CircularDurationView_secondsIndicatorTrackThickness, indicatorsTrackThickness.toFloat())
                    .roundToInt()
        mSecondsIndicatorTrackCornerRadius =
                attributes.getDimension(R.styleable.CircularDurationView_secondsIndicatorTrackCornerRadius, indicatorsTrackCornerRadius.toFloat())
                    .roundToInt()
        mSecondsIndicatorTrackGapSize =
                attributes.getDimension(R.styleable.CircularDurationView_secondsIndicatorTrackGapSize, indicatorsTrackGapSize.toFloat())
                    .roundToInt()
        attributes.recycle()

        mProgress = hoursIndicatorProgress.hours + minutesIndicatorProgress.minutes + secondsIndicatorProgress.seconds

        mHoursIndicator = CircularProgressIndicator(context)
        mMinutesIndicator = CircularProgressIndicator(context)
        mSecondsIndicator = CircularProgressIndicator(context)

        addView(mHoursIndicator)
        addView(mMinutesIndicator)
        addView(mSecondsIndicator)

        invalidate()
        requestLayout()
    }

    /**
     * Called to measure the size of the view.
     *
     * This method is responsible for determining the size of the view based on the layout constraints
     * and the content of the view.
     *
     * @param widthMeasureSpec  Horizontal space requirements as imposed by the parent. The requirements
     *                          are encoded with {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec Vertical space requirements as imposed by the parent. The requirements
     *                          are encoded with {@link android.view.View.MeasureSpec}.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val size = min(width, height)

        if (mIndicatorSize == -1) mIndicatorSize = size

        val viewWidth = mIndicatorSize + paddingStart + paddingEnd
        val viewHeight = mIndicatorSize + paddingTop + paddingBottom

        setMeasuredDimension(viewWidth, viewHeight)

        updateIndicators()
    }

    /**
     * Lays out the view and positions its child views.
     *
     * This method is called during layout when the size and position of the view's children
     * must be determined. It is responsible for placing the child views within the view's
     * boundaries based on the provided dimensions.
     *
     * @param changed A boolean indicating whether the layout has changed.
     * @param left The left position, relative to the parent.
     * @param top The top position, relative to the parent.
     * @param right The right position, relative to the parent.
     * @param bottom The bottom position, relative to the parent.
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) updateIndicators()
    }

    /**
     * Dispatches drawing operations to the view's child views.
     *
     * This function is responsible for drawing the view and its child views. It
     * is called by the Android system when the view needs to be redrawn.
     *
     * The function takes a single parameter, [canvas], which is the canvas onto
     * which the view should draw itself. The canvas is configured to draw within
     * the view's bounds.
     *
     * The function first calls the superclass implementation to draw the view's
     * background and then draws the view's text at the center of the view.
     *
     * @param canvas the canvas onto which the view should draw itself
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val text = mText.takeIf { it.isNotEmpty() } ?: mProgress.toFormattedTime()
        val availableTextWidth = mSecondsIndicator.indicatorSize - mSecondsIndicator.trackThickness * 2f
        mTextPaint.apply {
            color = mTextColor
            textAlign = mTextAlign
            textSize = availableTextWidth * 0.5f
            typeface = mTypeFace
        }

        var textWidth = mTextPaint.measureText(text)
        while (textWidth + (mTextPadding * 2f) > availableTextWidth) {
            mTextPaint.textSize -= 1f
            textWidth = mTextPaint.measureText(text)
        }
        val textHeight = mTextPaint.fontMetrics.descent + mTextPaint.fontMetrics.ascent

        val x = width * 0.5f
        val y = height * 0.5f - textHeight * 0.5f
        canvas.drawText(text, x, y, mTextPaint)

        if (isInfinite || !mShowSubText) return

        val nanos = mProgress.toComponents { _, _, _, nanoseconds -> nanoseconds / 1_000_000 }
        val subText = String.format(Locale.ENGLISH, ".%03d", nanos % 1_000)
        val subTextWidth = mTextPaint.measureText(subText)
        val subTextHeight = mTextPaint.fontMetrics.descent + mTextPaint.fontMetrics.ascent
        var subTextX = x + (textWidth * 0.5f) - (subTextWidth * 0.5f) - (mSecondsIndicatorTrackThickness * 0.5f) - mTextPadding
        val subTextY = y - subTextHeight + mSubTextPadding
        val secondsIndicatorRadius = mSecondsIndicator.indicatorSize * 0.5f - mSecondsIndicatorTrackThickness

        val textBounds = Rect()
        mTextPaint.getTextBounds(subText, 0, subText.length, textBounds)
        var left = subTextX - textBounds.width() * 0.5f
        var top = subTextY + textBounds.top
        var right = subTextX + textBounds.width() * 0.5f
        var bottom = subTextY + textBounds.bottom
        var rect = RectF(left, top, right, bottom)

        while (isTextBoundsOutsideRadius(rect, x, y + textHeight * 0.5f, secondsIndicatorRadius)) {
            mTextPaint.textSize -= 0.1f
            subTextX -= 0.1f

            mTextPaint.getTextBounds(subText, 0, subText.length, textBounds)
            left = subTextX - textBounds.width() * 0.5f
            top = subTextY + textBounds.top
            right = subTextX + textBounds.width() * 0.5f
            bottom = subTextY + textBounds.bottom
            rect = RectF(left, top, right, bottom)
        }

        canvas.drawText(subText, subTextX, subTextY, mTextPaint)
    }

    /**
     * Checks if the text bounds are outside the specified circle radius.
     * Loops through all possible angles and checks if any cartesian point of the circle is inside the text bounds.
     *
     * @param textBounds The bounds of the text.
     * @param circleX The x coordinate of the center of the circle.
     * @param circleY The y coordinate of the center of the circle.
     * @param radius The radius of the circle.
     *
     * @return [Boolean] `true` if the text bounds are outside the circle, `false` otherwise.
     */
    private fun isTextBoundsOutsideRadius(textBounds: RectF, circleX: Float, circleY: Float, radius: Float): Boolean {
        (0..360).forEach { angle ->
            val radians = Math.toRadians(angle.toDouble())
            val x = circleX + radius * cos(radians).toFloat()
            val y = circleY + radius * sin(radians).toFloat()

            if (x <= textBounds.right && x >= textBounds.left && y >= textBounds.top && y <= textBounds.bottom) return true
        }

        return false
    }

    /**
     * TODO: Fix this
     *
     * This method seems to work incorrectly even though the method body works perfectly in [dispatchDraw]
     * the issue is that it returns a RectF with a width double the actual text width
     */
    private fun getTextBounds(textStr: String, textX: Float, textY: Float, textPaint: Paint): RectF {
        val textBounds = Rect()
        textPaint.getTextBounds(textStr, 0, textStr.length, textBounds)

        val left = textX - textBounds.width() * 0.2f
        val top = textY + textBounds.top
        val right = textX + textBounds.width() * 0.5f
        val bottom = textY + textBounds.bottom

        return RectF(left, top, right, bottom)
    }

    /**
     * Updates the state and appearance of the indicators.
     *
     * This function is responsible for configuring and updating the properties
     * of each indicator, such as layout parameters, maximum values, sizes, colors,
     * and other visual attributes.
     */
    private fun updateIndicators() {
        mHoursIndicator.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)

            isIndeterminate = false
            max = mHoursIndicatorMax
            indicatorSize = mIndicatorSize
            trackColor = mHoursIndicatorTrackColor
            if (mHoursIndicatorTrackThickness != -1) trackThickness = mHoursIndicatorTrackThickness
            if (mHoursIndicatorTrackCornerRadius != -1) trackCornerRadius = mHoursIndicatorTrackCornerRadius
            if (mHoursIndicatorTrackGapSize != -1) indicatorTrackGapSize = mHoursIndicatorTrackGapSize
            showAnimationBehavior = CircularProgressIndicator.SHOW_OUTWARD
            setIndicatorColor(mHoursIndicatorColor)

            if (isInfinite) return@apply
            when {
                isInEditMode -> progress = mHoursIndicatorProgress
                else         -> setProgressCompat(mHoursIndicatorProgress, mIsAnimated)
            }
        }

        mMinutesIndicator.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)

            max = 59
            isIndeterminate = false
            indicatorSize = mIndicatorSize - (mHoursIndicatorTrackThickness * 2) - mIndicatorsGapSize
            trackColor = mMinutesIndicatorTrackColor
            if (mMinutesIndicatorTrackThickness != -1) trackThickness = mMinutesIndicatorTrackThickness
            if (mMinutesIndicatorTrackCornerRadius != -1) trackCornerRadius = mMinutesIndicatorTrackCornerRadius
            if (mMinutesIndicatorTrackGapSize != -1) indicatorTrackGapSize = mMinutesIndicatorTrackGapSize
            showAnimationBehavior = CircularProgressIndicator.SHOW_OUTWARD
            setIndicatorColor(mMinutesIndicatorColor)

            if (isInfinite) return@apply
            when {
                isInEditMode -> progress = mMinutesIndicatorProgress
                else         -> setProgressCompat(mMinutesIndicatorProgress, mIsAnimated)
            }
        }

        mSecondsIndicator.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)

            max = 59
            isIndeterminate = false
            indicatorSize = mIndicatorSize - (mHoursIndicatorTrackThickness * 2) - (mMinutesIndicatorTrackThickness * 2) - (mIndicatorsGapSize * 2)
            trackColor = mSecondsIndicatorTrackColor
            if (mSecondsIndicatorTrackThickness != -1) trackThickness = mSecondsIndicatorTrackThickness
            if (mSecondsIndicatorTrackCornerRadius != -1) trackCornerRadius = mSecondsIndicatorTrackCornerRadius
            if (mSecondsIndicatorTrackGapSize != -1) indicatorTrackGapSize = mSecondsIndicatorTrackGapSize
            showAnimationBehavior = CircularProgressIndicator.SHOW_OUTWARD
            setIndicatorColor(mSecondsIndicatorColor)

            if (isInfinite) return@apply
            when {
                isInEditMode -> progress = mSecondsIndicatorProgress
                else         -> setProgressCompat(mSecondsIndicatorProgress, mIsAnimated)
            }
        }

        if (mProgress.isFinite()) return

        mStaggerAnimationJob?.cancel()
        mStaggerAnimationJob = mLifecycleScope?.launch {
            mHoursIndicator.isIndeterminate = true
            delay(mStaggeredInfiniteAnimationDelay.milliseconds)
            mMinutesIndicator.isIndeterminate = true
            delay(mStaggeredInfiniteAnimationDelay.milliseconds)
            mSecondsIndicator.isIndeterminate = true
        }
    }

    /**
     * Converts the duration to a formatted string suitable for display to the user.
     *
     * The format of the string is determined by the current locale. If the locale is a right-to-left locale, the format will be `HH:mm:ss`.
     * Otherwise, the format will be `HH MM SS`.
     *
     * @param hideLegend [Boolean] if `true`, the string will not include the `hour`, `minute`, or `second` labels.
     *
     * @return [String] the formatted string.
     */
    private fun Duration.toFormattedTime(hideLegend: Boolean = false): String = toComponents { hours, minutes, seconds, _ ->
        when {
            isInfinite() -> "Ꝏ"
            else         -> {
                val format = when {
                    hideLegend -> if (hours == 0L) "%02d:%02d" else "%02d:%02d:%02d"
                    else       -> if (hours == 0L) "%02dm %02ds" else "%02dh %02dm %02ds"
                }

                when (hours) {
                    0L   -> String.format(Locale.ENGLISH, format, minutes, seconds)
                    else -> String.format(Locale.ENGLISH, format, hours, minutes, seconds)
                }
            }
        }
    }

    /**
     * A listener that is called when the view is attached to or detached from a window.
     *
     * This listener is used to attach the view to the lifecycle of the parent view.
     *
     * @author AbdAlMoniem AlHifnawy
     */
    private inner class AttachStateChangeListener : OnAttachStateChangeListener {

        /**
         * Called when the view is attached to a window.
         *
         * This method is called when the view is attached to a window, which means that the parent view
         * has a LifecycleOwner that can be used to attach the view to the lifecycle.
         *
         * @param view The view that is attached to a window.
         */
        override fun onViewAttachedToWindow(view: View) {
            view.findViewTreeLifecycleOwner()?.let { owner ->
                mLifecycleScope = owner.lifecycleScope
                Log.d(this@CircularDurationView::class.simpleName,"LifecycleOwner attached: ${owner::class.simpleName}@${owner.hashCode()}")
            } ?: Log.e(this@CircularDurationView::class.simpleName,"LifecycleOwner is null, ensure the parent has a LifecycleOwner.")
        }

        /**
         * Called when the view is detached from a window.
         *
         * This method is called when the view is detached from a window, which means that the parent view
         * no longer has a LifecycleOwner to attach the view to.
         *
         * @param view The view that is detached from a window.
         */
        override fun onViewDetachedFromWindow(view: View) {
            mLifecycleScope = null
        }
    }
}