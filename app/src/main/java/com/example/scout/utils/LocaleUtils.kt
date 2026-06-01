package com.example.scout.utils

import android.content.Context
import android.text.TextUtils
import android.view.View
import java.util.Locale

object LocaleUtils {

    fun apiLocale(): String = Locale.getDefault().language

    fun isRtl(): Boolean =
        TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}