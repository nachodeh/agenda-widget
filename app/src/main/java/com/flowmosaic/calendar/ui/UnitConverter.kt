package com.flowmosaic.calendar.ui

import android.content.Context
import android.util.TypedValue

object UnitConverter {

    fun spToPx(sp: Float, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        ).toInt()
    }
}