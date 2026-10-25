package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.SetRecord
import javax.inject.Inject

class UpdateSetUseCase @Inject constructor(
    private val repo: SessionRepository
) {
    suspend operator fun invoke(record: SetRecord) = repo.updateSetRecord(record)
}
