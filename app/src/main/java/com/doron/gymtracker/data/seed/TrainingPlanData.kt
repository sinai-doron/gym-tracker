package com.doron.gymtracker.data.seed

import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel

// Program: חזה וגב / רגליים / כתפיים וידיים
// Start date: 2026-10-26
// A/B variants per exercise: Phase 2. This seed uses the fixed exercise or Week A variant.
// Distance exercises (הליכת איכר): modeled as TimedExercise until Phase 2 adds distance type.
object TrainingPlanData {

    val workoutA = Workout(
        id = "workout_a",
        label = WorkoutLabel.A,
        exercises = listOf(
            // 1. Fixed — לחיצת חזה עם מוט (שטוח)
            RepExerciseDefinition("d1_e1", "לחיצת חזה עם מוט (שטוח)", targetSets = 4, restDurationSecs = 180, targetReps = 6),
            // 2. Fixed — פולי עליון אחיזה רחבה
            RepExerciseDefinition("d1_e2", "פולי עליון אחיזה רחבה", targetSets = 4, restDurationSecs = 120, targetReps = 6),
            // 3. A/B — Week A: לחיצת חזה עליון עם משקולות
            RepExerciseDefinition("d1_e3", "לחיצת חזה עליון עם משקולות", targetSets = 3, restDurationSecs = 90, targetReps = 10),
            // 4. A/B — Week A: חתירה עם מוט על ספסל (תמיכת חזה)
            RepExerciseDefinition("d1_e4", "חתירה עם מוט על ספסל (תמיכת חזה)", targetSets = 3, restDurationSecs = 120, targetReps = 8),
            // 5. A/B — Week A: פרפר במכונה
            RepExerciseDefinition("d1_e5", "פרפר במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 6. A/B — Week A: חתירה בכבל עם משולש
            RepExerciseDefinition("d1_e6", "חתירה בכבל עם משולש", targetSets = 3, restDurationSecs = 90, targetReps = 10),
            // 7. Fixed — פלאנק (hold 45-60s)
            TimedExerciseDefinition("d1_e7", "פלאנק", targetSets = 3, restDurationSecs = 60)
        )
    )

    val workoutB = Workout(
        id = "workout_b",
        label = WorkoutLabel.B,
        exercises = listOf(
            // 1. Fixed — סקוואט עם מוט חופשי
            RepExerciseDefinition("d2_e1", "סקוואט עם מוט חופשי", targetSets = 4, restDurationSecs = 180, targetReps = 6),
            // 2. A/B — Week A: דדליפט רומני עם מוט
            RepExerciseDefinition("d2_e2", "דדליפט רומני עם מוט", targetSets = 3, restDurationSecs = 120, targetReps = 8),
            // 3. A/B — Week A: סקוואט בולגרי (per side)
            RepExerciseDefinition("d2_e3", "סקוואט בולגרי", targetSets = 3, restDurationSecs = 90, targetReps = 10),
            // 4. Fixed — פשיטת ברכיים במכונה
            RepExerciseDefinition("d2_e4", "פשיטת ברכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 5. Fixed — כפיפת ברכיים במכונה
            RepExerciseDefinition("d2_e5", "כפיפת ברכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 6. Fixed — קירוב ירכיים במכונה (optional if short on time)
            RepExerciseDefinition("d2_e6", "קירוב ירכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 15),
            // 7. Fixed — תאומים בסמית' על מדרגה
            RepExerciseDefinition("d2_e7", "תאומים בסמית' על מדרגה", targetSets = 3, restDurationSecs = 60, targetReps = 15),
            // 8. A/B — Week A: כפיפות בטן בכבל
            RepExerciseDefinition("d2_e8", "כפיפות בטן בכבל", targetSets = 3, restDurationSecs = 60, targetReps = 12)
        )
    )

    val workoutC = Workout(
        id = "workout_c",
        label = WorkoutLabel.C,
        exercises = listOf(
            // 1. Fixed — לחיצת כתפיים עם משקולות בישיבה
            RepExerciseDefinition("d3_e1", "לחיצת כתפיים עם משקולות בישיבה", targetSets = 4, restDurationSecs = 120, targetReps = 8),
            // 2. A/B — Week A: הרחקה לצדדים עם משקולות
            RepExerciseDefinition("d3_e2", "הרחקה לצדדים עם משקולות", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 3. A/B — Week A: הרחקה אופקית במכונה (Reverse Pec Deck)
            RepExerciseDefinition("d3_e3", "הרחקה אופקית במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 4. A/B — Week A: יד קדמית עם מוט
            RepExerciseDefinition("d3_e4", "יד קדמית עם מוט", targetSets = 3, restDurationSecs = 90, targetReps = 8),
            // 5. A/B — Week A: יד אחורית בפולי עליון עם מוט
            RepExerciseDefinition("d3_e5", "יד אחורית בפולי עליון עם מוט", targetSets = 3, restDurationSecs = 90, targetReps = 10),
            // 6. Fixed — יד קדמית כנגד פולי תחתון
            RepExerciseDefinition("d3_e6", "יד קדמית כנגד פולי תחתון", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 7. Fixed — פשיטה מאחורי הראש עם משקולת (optional if short on time)
            RepExerciseDefinition("d3_e7", "פשיטה מאחורי הראש עם משקולת", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            // 8. Fixed — הליכת איכר (30-40m, modeled as timed until Phase 2 adds distance type)
            TimedExerciseDefinition("d3_e8", "הליכת איכר", targetSets = 3, restDurationSecs = 90)
        )
    )

    val all: List<Workout> = listOf(workoutA, workoutB, workoutC)
}
