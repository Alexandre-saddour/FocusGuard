package com.flowdevproduction.focusguard.domain.usecase

import com.flowdevproduction.focusguard.domain.FocusRepository
import javax.inject.Inject

class SetAnalyticsConsentUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAnalyticsEnabled(enabled)
}
