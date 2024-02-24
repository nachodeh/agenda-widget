package com.flowmosaic.calendar.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplitude.android.Amplitude
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.google.firebase.analytics.FirebaseAnalytics
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest=Config.NONE)
class AgendaWidgetLoggerTest {

    private lateinit var context: Context
    private lateinit var agendaWidgetLogger: AgendaWidgetLogger

    @Mock
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics

    @Mock
    private lateinit var mockAmplitude: Amplitude

    @Mock
    private lateinit var mockPrefs: AgendaWidgetPrefs

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        agendaWidgetLogger = AgendaWidgetLogger(mockAmplitude, mockFirebaseAnalytics, mockPrefs, context)
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun testLogActivityStartedEvent() {
        agendaWidgetLogger.logActivityStartedEvent(AgendaWidgetLogger.Activity.MAIN_ACTIVITY)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.ACTIVITY_STARTED.eventName), any())
    }

    @Test
    fun testLogNavigationEvent() {
        agendaWidgetLogger.logNavigationEvent("destination_screen")
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.NAVIGATION.eventName), any())
    }

    @Test
    fun testLogPermissionsResultEvent() {
        agendaWidgetLogger.logPermissionsResultEvent(true)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.PERMISSIONS_RESULT.eventName), any())
    }

    @Test
    fun testLogOnboardingCompleteEvent() {
        agendaWidgetLogger.logOnboardingCompleteEvent(true)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.ONBOARDING_COMPLETE.eventName), any())
    }

    @Test
    fun testLogActionButtonEvent() {
        agendaWidgetLogger.logActionButtonEvent(AgendaWidgetLogger.ActionButton.ADD_EVENT)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.ACTION_BUTTON.eventName), any())
    }

    @Test
    fun testLogUpdatePrefEvent() {
        agendaWidgetLogger.logUpdatePrefEvent(AgendaWidgetLogger.PrefsScreenItemName.DATE_SEPARATOR)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.UPDATE_PREF.eventName), any())
    }

    @Test
    fun testLogSelectItemEvent() {
        agendaWidgetLogger.logSelectItemEvent(AgendaWidgetLogger.WidgetItemName.EVENT)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.SELECT_ITEM.eventName), any())
    }

    @Test
    fun testLogWidgetLifecycleEvent() {
        agendaWidgetLogger.logWidgetLifecycleEvent(AgendaWidgetLogger.WidgetStatus.ACTIVE)
        verify(mockFirebaseAnalytics).logEvent(eq(AgendaWidgetLogger.Event.WIDGET_LIFECYCLE_EVENT.eventName), any())
    }

    @Test
    fun testFlushEvents() {
        agendaWidgetLogger.flushEvents()
        verify(mockAmplitude).flush()
        verifyNoMoreInteractions(mockAmplitude)
    }
}

