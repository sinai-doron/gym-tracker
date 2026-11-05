package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.entities.ProgramEntity
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.VariantRule
import java.time.LocalDate

internal fun Program.toEntity(isActive: Boolean) =
    ProgramEntity(
        id, name, startDate.toEpochDay(), deloadEveryWeeks, variantRule.name, isActive = isActive,
        deloadLoadPercent = deloadLoadPercent, deloadSets = deloadSets, variantEveryWeeks = variantEveryWeeks
    )

internal fun ProgramEntity.toDomain() = Program(
    id, name, LocalDate.ofEpochDay(startEpochDay), deloadEveryWeeks,
    runCatching { VariantRule.valueOf(variantRule) }.getOrDefault(VariantRule.NONE),
    deloadLoadPercent = deloadLoadPercent, deloadSets = deloadSets, variantEveryWeeks = variantEveryWeeks
)
