package com.example.focusguard.domain.usecase

import com.example.focusguard.domain.FocusRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBlockedAppsUseCase @Inject constructor(private val repository: FocusRepository) {
    operator fun invoke(): Flow<Set<String>> = repository.blockedPackages
}

class ToggleAppBlockUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(packageName: String, currentBlocked: Boolean) {
        if (currentBlocked) {
            repository.removeBlockedPackage(packageName)
        } else {
            repository.addBlockedPackage(packageName)
        }
    }
}

class GetFrictionSentenceUseCase @Inject constructor(private val repository: FocusRepository) {
    operator fun invoke(): Flow<String> = repository.frictionSentence
}

class UpdateFrictionSentenceUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(sentence: String) = repository.setFrictionSentence(sentence)
}

class GetAllowDurationUseCase @Inject constructor(private val repository: FocusRepository) {
    operator fun invoke(): Flow<Long> = repository.allowDuration
}

class UpdateAllowDurationUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(duration: Long) = repository.setAllowDuration(duration)
}

class GetGlobalServiceStateUseCase @Inject constructor(private val repository: FocusRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isServiceEnabled
}

class ToggleGlobalServiceStateUseCase @Inject constructor(private val repository: FocusRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setServiceEnabled(enabled)
}
