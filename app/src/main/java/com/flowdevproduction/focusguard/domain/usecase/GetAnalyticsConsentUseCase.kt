package com.flowdevproduction.focusguard.domain.usecase

import com.flowdevproduction.focusguard.domain.FocusRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetAnalyticsConsentUseCase @Inject constructor(private val repository: FocusRepository) {
    operator fun invoke(): Flow<Boolean?> = repository.isAnalyticsEnabled
}
