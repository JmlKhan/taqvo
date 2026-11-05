package com.taqvo.app.design

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

actual fun reduceMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
