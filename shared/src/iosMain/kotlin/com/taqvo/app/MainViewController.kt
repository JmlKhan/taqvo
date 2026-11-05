package com.taqvo.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import com.taqvo.app.crash.crashLogStore
import com.taqvo.app.crash.installCrashHandler
import com.taqvo.app.di.appContainer

fun MainViewController(): UIViewController {
    // Before the container exists, so that a crash while it is built is already recorded.
    installCrashHandler(crashLogStore)
    return ComposeUIViewController { App(appContainer) }
}
