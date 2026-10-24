package com.doron.gymtracker.data.seed

import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel

// Placeholder — replace with real exercises when provided by the user.
object TrainingPlanData {

    val workoutA = Workout(
        id = "workout_a",
        label = WorkoutLabel.A,
        exercises = listOf(
            RepExerciseDefinition("ex_a1", "Exercise A1 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 8),
            RepExerciseDefinition("ex_a2", "Exercise A2 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 10),
            TimedExerciseDefinition("ex_a3", "Plank", targetSets = 2, restDurationSecs = 60)
        )
    )

    val workoutB = Workout(
        id = "workout_b",
        label = WorkoutLabel.B,
        exercises = listOf(
            RepExerciseDefinition("ex_b1", "Exercise B1 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 8),
            RepExerciseDefinition("ex_b2", "Exercise B2 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 10)
        )
    )

    val workoutC = Workout(
        id = "workout_c",
        label = WorkoutLabel.C,
        exercises = listOf(
            RepExerciseDefinition("ex_c1", "Exercise C1 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 8),
            RepExerciseDefinition("ex_c2", "Exercise C2 (TBD)", targetSets = 3, restDurationSecs = 90, targetReps = 10)
        )
    )

    val all: List<Workout> = listOf(workoutA, workoutB, workoutC)
}
