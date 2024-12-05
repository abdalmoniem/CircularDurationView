package com.hifnawy.circulardurationview.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.PictureInPictureUiState
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.HapticFeedbackConstants
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.google.android.material.slider.Slider
import com.hifnawy.circulardurationview.Application
import com.hifnawy.circulardurationview.datastore.MutableListExtensionFunctions.addObserver
import com.hifnawy.circulardurationview.datastore.MutableListExtensionFunctions.removeObserver
import com.hifnawy.circulardurationview.datastore.SharedPrefsManager
import com.hifnawy.circulardurationview.datastore.SharedPrefsObserver
import com.hifnawy.circulardurationview.demo.R
import com.hifnawy.circulardurationview.demo.databinding.ActivityMainBinding
import com.hifnawy.circulardurationview.view.ActivityExtensionFunctions.setActivityTheme
import com.hifnawy.circulardurationview.view.ViewExtensionFunctions.onSizeChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : AppCompatActivity(), SharedPrefsObserver {

    /**
     * Lazily initializes the binding for the activity's layout using [ActivityMainBinding].
     *
     * This binding is used to access the views defined in the activity's layout XML file.
     * The layout is inflated using the [getLayoutInflater] provided by the activity.
     */
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    /**
     * Lazily retrieves the instance of [Application] associated with this activity.
     *
     * This property casts the [getApplication] context to [Application] and provides
     * access to application-wide resources and utilities.
     *
     * @return [Application] the application instance associated with this activity.
     */
    private val localApplication by lazy { application as Application }

    /**
     * Lazily initializes the shared preferences manager for the application using [SharedPrefsManager].
     *
     * This property provides access to the shared preferences of the application, which are used to store and retrieve values
     * related to the application's settings and state.
     *
     * @return [SharedPrefsManager] the shared preferences manager for the application.
     */
    private val sharedPreferences by lazy { SharedPrefsManager(localApplication) }

    /**
     * The count down timer instance used to count down the duration of the timer.
     *
     * This field is used to store the instance of [Job] that is used to count down the duration of the timer.
     *
     * @return [Job] the count down timer instance used to count down the duration of the timer.
     */
    private var countDownTimerJob: Job? = null

    /**
     * The flag indicating whether the count down timer is currently running.
     *
     * This field is used to store the state of the count down timer, indicating whether it is currently running or not.
     */
    private var isCountDownRunning = false

    /**
     * The size of the progress indicator.
     *
     * This field is used to store the size of the progress indicator, which is used to
     * determine the dimensions of the indicator when it is displayed on the screen.
     */
    private var progressIndicatorSize = 0

    /**
     * A lazily initialized instance of [PictureInPictureParams.Builder] that is used to enter picture-in-picture mode.
     *
     * This field is used to store the [PictureInPictureParams.Builder] that is used to enter picture-in-picture mode.
     * It is initialized lazily when it is first accessed. The instance is created using the
     * [PictureInPictureParams.Builder] constructor and the [Rational] constructor.
     * The aspect ratio of the builder is set to 1:1 to ensure that the activity is displayed in a square aspect ratio.
     *
     * @return [PictureInPictureParams.Builder] the instance of [PictureInPictureParams.Builder] that is used to enter picture-in-picture mode.
     */
    private val pipParamsBuilder by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@lazy null

        PictureInPictureParams.Builder().apply {
            pipSourceRectHint = Rect()
            binding.progressIndicator.getGlobalVisibleRect(pipSourceRectHint)

            setSourceRectHint(pipSourceRectHint)
            setAspectRatio(Rational(1, 1))

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@apply
            setSeamlessResizeEnabled(false)
        }
    }

    /**
     * The source rectangle hint to use when entering picture-in-picture mode.
     *
     * This field is used to store the source rectangle hint that is passed to the
     * [PictureInPictureParams.Builder] when entering picture-in-picture mode.
     * It is used to specify the region of the screen that the activity is currently using.
     *
     * @see PictureInPictureParams.Builder.setSourceRectHint
     */
    private lateinit var pipSourceRectHint: Rect

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState [Bundle] If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     *     Note: Otherwise it is null.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences.run { setActivityTheme(theme.mode, isMaterialYouEnabled) }

        enableEdgeToEdge()
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            insets
        }

        window.enterTransition = null
        window.exitTransition = null

        with(binding) {
            settingsCardLayout.children.forEach { childView ->
                childView.isVisible = false
            }

            pipParamsBuilder?.run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@run

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(sharedPreferences.isPictureInPictureEnabled && isCountDownRunning)
                setPictureInPictureParams(build())
            }

            addOnPictureInPictureModeChangedListener { pipModeInfo ->
                toolbar.isVisible = !pipModeInfo.isInPictureInPictureMode
                settingsCard.isVisible = !pipModeInfo.isInPictureInPictureMode
                settingsCardTitle.isVisible = !pipModeInfo.isInPictureInPictureMode

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@addOnPictureInPictureModeChangedListener

                pipParamsBuilder?.run {
                    setPictureInPictureParams(build())
                }
            }

            progressIndicator.post {
                progressIndicatorSize = progressIndicator.indicatorSize

                root.onSizeChange { _, newWidth, newHeight, _, _ ->
                    Log.d(this@MainActivity::class.simpleName, "root layout size changed, newWidth: $newWidth, newHeight: $newHeight")

                    with(progressIndicator) {
                        indicatorSize = when {
                            isInPictureInPictureMode -> min(newWidth, newHeight) - paddingStart - paddingEnd
                            else                     -> progressIndicatorSize
                        }

                        pipParamsBuilder?.run {
                            val length = min(measuredWidth, measuredHeight) / 2
                            pipSourceRectHint = Rect(0, 0, length, length)

                            setAspectRatio(Rational(1, 1))
                            setSourceRectHint(pipSourceRectHint)

                            setPictureInPictureParams(build())
                        }
                    }
                }
            }

            progressIndicatorHoursSlider.addTouchAnimations()
            progressIndicatorMinutesSlider.addTouchAnimations()
            progressIndicatorSecondsSlider.addTouchAnimations()

            progressIndicatorHoursSlider.addOnChangeListener { slider, value, _ ->
                slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                progressIndicator.hoursIndicatorProgress = value.toInt()
            }

            progressIndicatorMinutesSlider.addOnChangeListener { slider, value, _ ->
                slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                progressIndicator.minutesIndicatorProgress = value.toInt()
            }

            progressIndicatorSecondsSlider.addOnChangeListener { slider, value, _ ->
                slider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                progressIndicator.secondsIndicatorProgress = value.toInt()
            }

            toggleButton.setOnClickListener { button ->
                button.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                if (!isCountDownRunning) {
                    isCountDownRunning = true

                    countDownTimerJob = startCountdown(progressIndicator.progress, 50.milliseconds, { remaining ->
                        progressIndicator.progress = remaining
                    }) {
                        isCountDownRunning = false
                        countDownTimerJob?.cancel()
                        countDownTimerJob = null

                        changeProgressIndicatorControls(true)
                        progressIndicator.showSubText = false
                        infiniteToggleButton.isEnabled = !isCountDownRunning
                        toggleButton.text = when {
                            isCountDownRunning -> getString(R.string.stop)
                            else               -> getString(R.string.start)
                        }
                    }
                    countDownTimerJob?.start()
                } else {
                    isCountDownRunning = false
                    countDownTimerJob?.cancel()
                    countDownTimerJob = null
                }

                changeProgressIndicatorControls(!isCountDownRunning)
                progressIndicator.showSubText = isCountDownRunning
                infiniteToggleButton.isEnabled = !isCountDownRunning
                toggleButton.text = when {
                    isCountDownRunning -> getString(R.string.stop)
                    else               -> getString(R.string.start)
                }
            }

            infiniteToggleButton.setOnClickListener { button ->
                button.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                isCountDownRunning = !isCountDownRunning

                if (isCountDownRunning) progressIndicator.progress = INFINITE
                changeProgressIndicatorControls(!isCountDownRunning)

                toggleButton.isEnabled = !isCountDownRunning
                infiniteToggleButton.text = when {
                    isCountDownRunning -> getString(R.string.stop)
                    else               -> getString(R.string.infinite)
                }

                progressIndicator.showSubText = false
            }

            lifecycleScope.launch {
                settingsCardLayout.children.forEach { childView ->
                    childView.isVisible = true
                    delay(150.milliseconds)
                }
            }
        }
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for your activity to start interacting with the user. This is a good place to
     * begin animations, open exclusive-access devices (such as the camera), etc.
     *
     * Keep in mind that onResume is not the best indicator that your activity is visible to the user (as described in the ActivityLifecycle document).
     *
     * @see [onPause]
     * @see [onStop]
     * @see [onDestroy]
     */
    override fun onResume() {
        super.onResume()

        changeMaterialYouPreferences(DynamicColors.isDynamicColorAvailable())
        changePictureInPicturePreferences(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)

        with(binding) {
            toolbar.isVisible = !isInPictureInPictureMode
            settingsCard.isVisible = !isInPictureInPictureMode
            settingsCardTitle.isVisible = !isInPictureInPictureMode

            appThemeSelectionView.run {
                val themeButtons = mapOf(
                        appThemeSystemDefaultButton to SharedPrefsManager.Theme.SYSTEM_DEFAULT,
                        appThemeSystemLightButton to SharedPrefsManager.Theme.LIGHT,
                        appThemeSystemDarkButton to SharedPrefsManager.Theme.DARK
                )
                val selectedThemeButtonId = when (sharedPreferences.theme) {
                    SharedPrefsManager.Theme.SYSTEM_DEFAULT -> appThemeSystemDefaultButton.id
                    SharedPrefsManager.Theme.LIGHT          -> appThemeSystemLightButton.id
                    SharedPrefsManager.Theme.DARK           -> appThemeSystemDarkButton.id
                }

                appThemeToggleGroup.check(selectedThemeButtonId)

                themeButtons.forEach { entry ->
                    entry.key.setOnClickListener { button ->
                        button.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val theme = entry.value

                        if (theme == sharedPreferences.theme) return@setOnClickListener

                        sharedPreferences.run { changeThemeAndContrast(theme, isMaterialYouEnabled) }
                    }
                }
            }
        }

        localApplication.sharedPrefsObservers.addObserver(this@MainActivity)
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been destroyed. Use this method to
     * release resources, such as broadcast receivers, that will not be needed while the activity is paused.
     *
     * This is usually a good place to commit unsaved changes to persistent data, stop animations and other ongoing actions, etc.
     *
     * @see [onResume]
     * @see [onStop]
     * @see [onDestroy]
     */
    override fun onPause() {
        super.onPause()

        if (sharedPreferences.isPictureInPictureEnabled) return

        localApplication.sharedPrefsObservers.removeObserver(this@MainActivity)
    }

    /**
     * Called when the user is giving a hint that they are leaving your activity.
     *
     * This is called when the user presses the home button, or when the user starts navigating away from your activity.
     * This is used by the system to determine whether to enter picture-in-picture mode.
     *
     * @see [onPictureInPictureModeChanged]
     * @see [isInPictureInPictureMode]
     * @see [enterPictureInPictureMode]
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!sharedPreferences.isPictureInPictureEnabled || !isCountDownRunning) return

        pipParamsBuilder?.run { enterPictureInPictureMode(build()) }
    }

    /**
     * Called when the picture in picture mode's UI state has changed.
     *
     * @param pipState [PictureInPictureUiState] The current state of the picture in picture mode's UI.
     *
     * @see PictureInPictureUiState
     */
    override fun onPictureInPictureUiStateChanged(pipState: PictureInPictureUiState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        if (!pipState.isTransitioningToPip) return

        binding.toolbar.isVisible = false
        binding.settingsCard.isVisible = false
        binding.settingsCardTitle.isVisible = false
    }

    /**
     * Called when the "Picture in Picture Enabled" preference changes.
     *
     * @param isPictureInPictureEnabled [Boolean] `true` if the "Picture in Picture" feature is enabled, `false` otherwise.
     */
    override fun onIsPictureInPictureEnabledUpdated(isPictureInPictureEnabled: Boolean) {
        binding.pictureInPictureSwitch.isChecked = isPictureInPictureEnabled

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        with(binding.progressIndicator) {
            pipParamsBuilder?.run {
                val length = min(measuredWidth, measuredHeight) / 2
                pipSourceRectHint = Rect(0, 0, length, length)

                setAspectRatio(Rational(1, 1))
                setSourceRectHint(pipSourceRectHint)
                setAutoEnterEnabled(isPictureInPictureEnabled && isCountDownRunning)

                setPictureInPictureParams(build())
            }
        }
    }

    /**
     * Changes the theme and contrast level of the application.
     *
     * This function takes the given [selectedTheme], and
     * [selectedIsMaterialYouEnabled] values and applies them to the application.
     * It updates the [SharedPrefsManager.theme] and [SharedPrefsManager.isMaterialYouEnabled]
     * properties of the [sharedPreferences] to reflect the new values.
     *
     * @param selectedTheme The new theme to apply to the application.
     * @param selectedIsMaterialYouEnabled Whether to enable Material You theme or not.
     *
     * @see SharedPrefsManager.Theme
     * @see SharedPrefsManager.theme
     * @see SharedPrefsManager.isMaterialYouEnabled
     */
    private fun changeThemeAndContrast(selectedTheme: SharedPrefsManager.Theme, selectedIsMaterialYouEnabled: Boolean) = sharedPreferences.run {
        val isToRecreate = theme == selectedTheme

        theme = selectedTheme
        isMaterialYouEnabled = selectedIsMaterialYouEnabled

        when {
            isToRecreate -> recreate()
            else         -> delegate.localNightMode = selectedTheme.mode
        }
    }

    /**
     * Enables the Material You preferences.
     *
     * The Material You preferences are:
     * 1. A [MaterialCardView][com.google.android.material.card.MaterialCardView] that shows the Material You preferences.
     * 2. A [TextView][android.widget.TextView] that shows the Material You preferences title.
     * 3. A [TextView][android.widget.TextView] that shows the Material You preferences subtitle.
     * 4. A [MaterialSwitch][com.google.android.material.materialswitch.MaterialSwitch] that toggles the Material You preferences..
     *
     * @param isEnabled [Boolean] `true` if the Material You preferences should be enabled, `false` otherwise.
     */
    private fun changeMaterialYouPreferences(isEnabled: Boolean) {
        with(binding) {
            materialYouCard.isEnabled = isEnabled
            materialYouTextView.isEnabled = isEnabled
            materialYouSubTextTextView.isEnabled = isEnabled
            materialYouSubTextTextView.isVisible = !isEnabled || sharedPreferences.isMaterialYouEnabled
            materialYouSubTextTextView.text = when {
                isEnabled -> getString(R.string.material_you_pref_description)
                else      -> getString(R.string.material_you_pref_not_available)
            }
            materialYouSwitch.isEnabled = isEnabled
            materialYouSwitch.isChecked = sharedPreferences.isMaterialYouEnabled

            materialYouCard.setOnClickListener { materialYouSwitch.isChecked = !sharedPreferences.isMaterialYouEnabled }
            materialYouSwitch.setOnCheckedChangeListener { switch, isChecked ->
                switch.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                materialYouSubTextTextView.isEnabled = isChecked
                materialYouSubTextTextView.isVisible = isChecked

                sharedPreferences.run { changeThemeAndContrast(theme, isChecked) }
            }
        }
    }

    /**
     * Enables the picture in picture preferences.
     *
     * The picture in picture preferences are:
     * 1. A [MaterialCardView][com.google.android.material.card.MaterialCardView] that shows the picture in picture preferences.
     * 2. A [TextView][android.widget.TextView] that shows the picture in picture preferences title.
     * 3. A [TextView][android.widget.TextView] that shows the picture in picture preferences subtitle.
     * 4. A [MaterialSwitch][com.google.android.material.materialswitch.MaterialSwitch] that toggles the picture in picture preferences.
     *
     * @param isEnabled [Boolean] `true` if the picture in picture preferences should be enabled, `false` otherwise.
     */
    private fun changePictureInPicturePreferences(isEnabled: Boolean) {
        with(binding) {
            pictureInPictureCard.isEnabled = isEnabled
            pictureInPictureTextView.isEnabled = isEnabled
            pictureInPictureSubTextTextView.isEnabled = isEnabled && sharedPreferences.isPictureInPictureEnabled
            pictureInPictureSubTextTextView.isVisible = isEnabled && sharedPreferences.isPictureInPictureEnabled
            pictureInPictureSwitch.isEnabled = isEnabled
            pictureInPictureSwitch.isChecked = sharedPreferences.isPictureInPictureEnabled

            pictureInPictureCard.setOnClickListener { pictureInPictureSwitch.isChecked = !sharedPreferences.isPictureInPictureEnabled }
            pictureInPictureSwitch.setOnCheckedChangeListener { switch, isChecked ->
                switch.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                pictureInPictureSubTextTextView.isEnabled = isChecked
                pictureInPictureSubTextTextView.isVisible = isChecked
                sharedPreferences.isPictureInPictureEnabled = isChecked
            }
        }
    }

    /**
     * Enables or disables the progress indicator controls.
     *
     * @param isEnabled [Boolean] `true` if the progress indicator controls should be enabled, `false` otherwise.
     */
    private fun changeProgressIndicatorControls(isEnabled: Boolean) = with(binding) {
        progressIndicatorControlsCard.isEnabled = isEnabled
        progressIndicatorControlsLayout.children.forEach { childView ->
            childView.isEnabled = isEnabled
        }

        if (isEnabled) {
            progressIndicator.hoursIndicatorProgress = progressIndicatorHoursSlider.value.roundToInt()
            progressIndicator.minutesIndicatorProgress = progressIndicatorMinutesSlider.value.roundToInt()
            progressIndicator.secondsIndicatorProgress = progressIndicatorSecondsSlider.value.roundToInt()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@with
        with(binding.progressIndicator) {
            pipParamsBuilder?.run {
                val length = min(measuredWidth, measuredHeight) / 2
                pipSourceRectHint = Rect(0, 0, length, length)

                setAspectRatio(Rational(1, 1))
                setSourceRectHint(pipSourceRectHint)
                setAutoEnterEnabled(sharedPreferences.isPictureInPictureEnabled && isCountDownRunning)

                setPictureInPictureParams(build())
            }
        }
    }

    private fun startCountdown(
            duration: Duration,
            interval: Duration = 50.milliseconds,
            onTick: (remaining: Duration) -> Unit,
            onFinish: () -> Unit
    ): Job = lifecycleScope.launch {
        generateSequence(duration) { it - interval }.takeWhile { it >= Duration.ZERO }.forEach { remaining ->
            onTick(remaining)
            delay(interval.inWholeMilliseconds)
        }
        onFinish()
    }

    /**
     * Adds touch animations to this slider.
     *
     * This function adds animations when the user touches the slider. When the user
     * touches the slider, the track height will animate to 40.dp and the thumb height
     * will animate to 50.dp. When the user releases the touch, the track height will
     * animate back to 20.dp and the thumb height will animate back to 30.dp.
     *
     * @param heightMin The minimum height of the track and thumb. Defaults to 20.dp.
     * @param heightMax The maximum height of the track and thumb. Defaults to 80.dp.
     * @param animationDuration The duration of the animation. Defaults to 300 milliseconds.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun Slider.addTouchAnimations(heightMin: Int = 20.dp, heightMax: Int = 60.dp, animationDuration: Duration = 150.milliseconds) {
        addOnSliderTouchListener(
                object : Slider.OnSliderTouchListener {
                    @SuppressLint("Recycle")
                    val animator = ValueAnimator().apply {
                        duration = animationDuration.inWholeMilliseconds

                        addUpdateListener {
                            trackHeight = it.animatedValue as Int
                            thumbHeight = it.animatedValue as Int
                        }
                    }

                    override fun onStartTrackingTouch(slider: Slider) = animator.run {
                        setIntValues(heightMin, heightMax)
                        start()
                    }

                    override fun onStopTrackingTouch(slider: Slider) = animator.run {
                        setIntValues(heightMax, heightMin)
                        start()
                    }
                }
        )
    }

    private val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()
}