package com.flowmosaic.calendar

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class AgendaWidgetApplication: Application() {

    companion object {
        const val POSTHOG_API_KEY = BuildConfig.POSTHOG_API_KEY
        const val POSTHOG_HOST = "https://us.i.posthog.com"
    }

    override fun onCreate() {
        super.onCreate()

        // Create a PostHog Config with the given API key and host
        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host = POSTHOG_HOST
        ).apply {
            sessionReplay = true
        }


        // Setup PostHog with the given Context and Config
        PostHogAndroid.setup(this, config)
    }
}