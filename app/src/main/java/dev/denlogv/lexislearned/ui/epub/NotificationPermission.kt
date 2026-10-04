package dev.denlogv.lexislearned.ui.epub

/** The first Android version that lets the user decide whether an app may show notifications (Android 13). */
private const val NOTIFICATION_PERMISSION_SDK = 33

/**
 * Whether to ask for the permission to show notifications before generating a deck. The progress notification of the background
 * service is part of what the user should see, but generation works without it, so the answer never blocks anything.
 *
 * @param sdk the Android version of the device.
 * @param granted whether the permission is granted already.
 * @return true on Android 13 and later if it is not granted.
 */
fun shouldAskForNotifications(sdk: Int, granted: Boolean): Boolean = sdk >= NOTIFICATION_PERMISSION_SDK && !granted
