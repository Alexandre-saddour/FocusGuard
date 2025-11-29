package com.example.focusguard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusguard.domain.usecase.CheckAppUsageUseCase
import com.example.focusguard.domain.usecase.GetAllowDurationUseCase
import com.example.focusguard.domain.usecase.GetFrictionSentenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FrictionViewModel
@Inject
constructor(
        private val getFrictionSentenceUseCase: GetFrictionSentenceUseCase,
        private val getAllowDurationUseCase: GetAllowDurationUseCase,
        private val checkAppUsageUseCase: CheckAppUsageUseCase
) : ViewModel() {

    val frictionSentence =
            getFrictionSentenceUseCase()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allowDuration =
            getAllowDurationUseCase()
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L)

    fun unlockApp(packageName: String, duration: Long) {
        checkAppUsageUseCase.temporarilyAllowPackage(packageName, duration)
    }
}
