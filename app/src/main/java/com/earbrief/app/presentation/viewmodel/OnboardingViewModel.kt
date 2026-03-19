package com.earbrief.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("earbrief_prefs", 0)

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_COMPLETE, false)

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    companion object {
        private const val KEY_COMPLETE = "onboarding_complete"
    }
}
