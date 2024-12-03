# CircularDurationView

A custom view for Android that displays a duration in a circular fashion.

[![](https://jitpack.io/v/hifnawy/CircularDurationView.svg)](https://jitpack.io/#hifnawy/CircularDurationView)

## Screenshot

![screenshot](screenshots/screenshot.png)

## Features

- Displays a duration in a circular format.
- The duration can be in seconds, minutes or hours.
- The progress indicators can be animated or not.
- The view is divided into three parts: hours, minutes and seconds.
- For each part, a progress indicator is displayed to show the progress of that part of the duration.
- A text view is displayed at the center of the view to show the duration in a readable format.

## Attributes

| Attribute                         | Type      | Description                                                | Default Value |
|-----------------------------------|-----------|------------------------------------------------------------|---------------|
| indicatorSize                     | dimension | The size of the progress indicators.                       | undefined     |
| indicatorsGapSize                 | dimension | The gap size between the progress indicators.              | 5dp           |
| indicatorsColor                   | color     | The color of the progress indicators.                      | colorPrimary  |
| indicatorsTrackColor              | color     | The color of the track of the progress indicators.         | colorPrimary  |
| indicatorsTrackGapSize            | dimension | The gap size between the track of the progress indicators. | 10dp          |
| indicatorsTrackThickness          | dimension | The thickness of the track of the progress indicators.     | 15dp          |
| indicatorsTrackCornerRadius       | dimension | The corner radius of the track of the progress indicators. | 10dp          |
| animated                          | boolean   | Whether the progress indicators are animated or not.       | true          |
| hoursIndicatorMax                 | integer   | The maximum value of the hours indicator.                  | 24            |
| hoursIndicatorProgress            | integer   | The progress of the hours indicator.                       | 0             |
| hoursIndicatorColor               | color     | The color of the hours indicator.                          | colorPrimary  |
| hoursIndicatorTrackColor          | color     | The color of the track of the hours indicator.             | colorPrimary  |
| hoursIndicatorTrackGapSize        | dimension | The gap size between the track of the hours indicator.     | undefined     |
| hoursIndicatorTrackThickness      | dimension | The thickness of the track of the hours indicator.         | undefined     |
| hoursIndicatorTrackCornerRadius   | dimension | The corner radius of the track of the hours indicator.     | undefined     |
| minutesIndicatorProgress          | integer   | The progress of the minutes indicator.                     | 0             |
| minutesIndicatorColor             | color     | The color of the minutes indicator.                        | colorPrimary  |
| minutesIndicatorTrackColor        | color     | The color of the track of the minutes indicator.           | colorPrimary  |
| minutesIndicatorTrackGapSize      | dimension | The gap size between the track of the minutes indicator.   | undefined     |
| minutesIndicatorTrackThickness    | dimension | The thickness of the track of the minutes indicator.       | undefined     |
| minutesIndicatorTrackCornerRadius | dimension | The corner radius of the track of the minutes indicator.   | undefined     |
| secondsIndicatorProgress          | integer   | The progress of the seconds indicator.                     | 0             |
| secondsIndicatorColor             | color     | The color of the seconds indicator.                        | colorPrimary  |
| secondsIndicatorTrackColor        | color     | The color of the track of the seconds indicator.           | colorPrimary  |
| secondsIndicatorTrackGapSize      | dimension | The gap size between the track of the seconds indicator.   | undefined     |
| secondsIndicatorTrackThickness    | dimension | The thickness of the track of the seconds indicator.       | undefined     |
| secondsIndicatorTrackCornerRadius | dimension | The corner radius of the track of the seconds indicator.   | undefined     |
| text                              | string    | The text to be displayed at the center of the view.        | undefined     |
| textColor                         | color     | The color of the text.                                     | colorPrimary  |
| textStyle                         | flags     | The style of the text.                                     | normal        |
| textAlign                         | enum      | The alignment of the text.                                 | center        |
| textPadding                       | dimension | The padding of the text.                                   | 0dp           |
| textFontFamily                    | reference | The font family of the text.                               | undefined     |

## Usage

Add the following dependency to your build.gradle file:

### Gradle
```gradle
dependencies {
    implementation 'com.hifnawy:circulardurationview:1.0.0'
}
```

### Kotlin DSL
```kotlin
dependencies {
    implementation("com.hifnawy:circulardurationview:1.0.0")
}
```

### XML layout
```xml
<com.hifnawy.circulardurationview.CircularDurationView
    android:id="@+id/progressIndicator"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="20dp"
    app:hoursIndicatorColor="?colorPrimary"
    app:hoursIndicatorMax="24"
    app:hoursIndicatorProgress="7"
    app:hoursIndicatorTrackColor="?colorPrimaryContainer"
    app:indicatorSize="350dp"
    app:indicatorsGapSize="5dp"
    app:indicatorsTrackCornerRadius="30dp"
    app:indicatorsTrackGapSize="10dp"
    app:indicatorsTrackThickness="10dp"
    app:layout_constraintBottom_toTopOf="@+id/settingsCardTitle"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintVertical_weight="2"
    app:minutesIndicatorColor="?colorSecondary"
    app:minutesIndicatorProgress="30"
    app:minutesIndicatorTrackColor="?colorSecondaryContainer"
    app:secondsIndicatorColor="?colorTertiary"
    app:secondsIndicatorProgress="40"
    app:secondsIndicatorTrackColor="?colorTertiaryContainer"
    app:textAlign="center"
    app:textColor="?colorPrimary"
    app:textFontFamily="@font/manrope"
    app:textPadding="5dp"
    app:textStyle="normal" />
```

### From Code
```kotlin
val progressIndicator = findViewById<CircularDurationView>(R.id.progressIndicator)
progressIndicator.hoursIndicatorProgress = 7
progressIndicator.minutesIndicatorProgress = 30
progressIndicator.secondsIndicatorProgress = 40
progressIndicator.text = "00:00:00"
progressIndicator.progress = 100.minutes
...
```
