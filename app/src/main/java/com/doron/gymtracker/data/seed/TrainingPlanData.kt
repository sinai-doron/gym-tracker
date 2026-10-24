package com.doron.gymtracker.data.seed

import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel

// Programme: חזה וגב / רגליים / כתפיים וידיים
// Start date: 2026-10-26. variantB = Week B exercise; null = fixed.
object TrainingPlanData {

    val workoutA = Workout(
        id = "workout_a",
        label = WorkoutLabel.A,
        exercises = listOf(
            RepExerciseDefinition("d1_e1", "לחיצת חזה עם מוט (שטוח)", targetSets = 4, restDurationSecs = 180, targetReps = 6),
            RepExerciseDefinition("d1_e2", "פולי עליון אחיזה רחבה", targetSets = 4, restDurationSecs = 120, targetReps = 6),
            RepExerciseDefinition(
                id = "d1_e3a", name = "לחיצת חזה עליון עם משקולות",
                targetSets = 3, restDurationSecs = 90, targetReps = 10,
                variantB = RepExerciseDefinition("d1_e3b", "לחיצת חזה עליון עם מוט", 3, 90, 10)
            ),
            RepExerciseDefinition(
                id = "d1_e4a", name = "חתירה עם מוט על ספסל (תמיכת חזה)",
                targetSets = 3, restDurationSecs = 120, targetReps = 8,
                variantB = RepExerciseDefinition("d1_e4b", "חתירה עם משקולת ביד אחת", 3, 120, 8)
            ),
            RepExerciseDefinition(
                id = "d1_e5a", name = "פרפר במכונה",
                targetSets = 3, restDurationSecs = 60, targetReps = 12,
                variantB = RepExerciseDefinition("d1_e5b", "פרפר בפולי תחתון", 3, 60, 12)
            ),
            RepExerciseDefinition(
                id = "d1_e6a", name = "חתירה בכבל עם משולש",
                targetSets = 3, restDurationSecs = 90, targetReps = 10,
                variantB = RepExerciseDefinition("d1_e6b", "חתירה בכבל עם מוט ישר", 3, 90, 10)
            ),
            TimedExerciseDefinition("d1_e7", "פלאנק", targetSets = 3, restDurationSecs = 60)
        )
    )

    val workoutB = Workout(
        id = "workout_b",
        label = WorkoutLabel.B,
        exercises = listOf(
            RepExerciseDefinition("d2_e1", "סקוואט עם מוט חופשי", targetSets = 4, restDurationSecs = 180, targetReps = 6),
            RepExerciseDefinition(
                id = "d2_e2a", name = "דדליפט רומני עם מוט",
                targetSets = 3, restDurationSecs = 120, targetReps = 8,
                variantB = RepExerciseDefinition("d2_e2b", "דדליפט רומני עם משקולות", 3, 120, 8)
            ),
            RepExerciseDefinition(
                id = "d2_e3a", name = "סקוואט בולגרי",
                targetSets = 3, restDurationSecs = 90, targetReps = 10,
                isPerSide = true,
                variantB = RepExerciseDefinition("d2_e3b", "לאנג'ים בהליכה עם משקולות", 3, 90, 10, isPerSide = true)
            ),
            RepExerciseDefinition("d2_e4", "פשיטת ברכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            RepExerciseDefinition("d2_e5", "כפיפת ברכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            RepExerciseDefinition("d2_e6", "קירוב ירכיים במכונה", targetSets = 3, restDurationSecs = 60, targetReps = 15, isOptional = true),
            RepExerciseDefinition("d2_e7", "תאומים בסמית' על מדרגה", targetSets = 3, restDurationSecs = 60, targetReps = 15),
            RepExerciseDefinition(
                id = "d2_e8a", name = "כפיפות בטן בכבל",
                targetSets = 3, restDurationSecs = 60, targetReps = 12,
                variantB = RepExerciseDefinition("d2_e8b", "הרמות ברכיים בתלייה", 3, 60, 12)
            )
        )
    )

    val workoutC = Workout(
        id = "workout_c",
        label = WorkoutLabel.C,
        exercises = listOf(
            RepExerciseDefinition("d3_e1", "לחיצת כתפיים עם משקולות בישיבה", targetSets = 4, restDurationSecs = 120, targetReps = 8),
            RepExerciseDefinition(
                id = "d3_e2a", name = "הרחקה לצדדים עם משקולות",
                targetSets = 3, restDurationSecs = 60, targetReps = 12,
                variantB = RepExerciseDefinition("d3_e2b", "הרחקה לצדדים בכבל", 3, 60, 12)
            ),
            RepExerciseDefinition(
                id = "d3_e3a", name = "הרחקה אופקית במכונה",
                targetSets = 3, restDurationSecs = 60, targetReps = 12,
                variantB = RepExerciseDefinition("d3_e3b", "Face Pull בכבל", 3, 60, 12)
            ),
            RepExerciseDefinition(
                id = "d3_e4a", name = "יד קדמית עם מוט",
                targetSets = 3, restDurationSecs = 90, targetReps = 8,
                variantB = RepExerciseDefinition("d3_e4b", "יד קדמית פטיש עם משקולות", 3, 90, 8)
            ),
            RepExerciseDefinition(
                id = "d3_e5a", name = "יד אחורית בפולי עליון עם מוט",
                targetSets = 3, restDurationSecs = 90, targetReps = 10,
                variantB = RepExerciseDefinition("d3_e5b", "יד אחורית בפולי עליון עם חבל", 3, 90, 10)
            ),
            RepExerciseDefinition("d3_e6", "יד קדמית כנגד פולי תחתון", targetSets = 3, restDurationSecs = 60, targetReps = 12),
            RepExerciseDefinition("d3_e7", "פשיטה מאחורי הראש עם משקולת", targetSets = 3, restDurationSecs = 60, targetReps = 12, isOptional = true),
            DistanceExerciseDefinition("d3_e8", "הליכת איכר", targetSets = 3, restDurationSecs = 90, targetDistanceM = 35)
        )
    )

    val all: List<Workout> = listOf(workoutA, workoutB, workoutC)
}
