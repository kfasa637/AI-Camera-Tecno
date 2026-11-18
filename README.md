# AI Camera for TECNO Spark 20 Pro+

Android camera project built with Kotlin and CameraX.

## Planned features
- Camera preview, photo capture, flash and front/back camera switching
- Digital zoom with an enhancement pipeline
- Dedicated macro mode and focus assistance
- Original photo is saved immediately; opening its thumbnail starts enhancement and then displays the processed result
- Original/processed comparison and separate saving
- GitHub Actions APK build

## Build
Open this repository in Android Studio and run the `app` configuration, or use the Actions workflow to build a debug APK.

## Important
The first version establishes the Android project and camera pipeline. Genuine AI super-resolution requires a compatible on-device model; the app must not label ordinary sharpening or filters as AI. Device-specific camera capabilities vary by hardware and Android camera HAL.
