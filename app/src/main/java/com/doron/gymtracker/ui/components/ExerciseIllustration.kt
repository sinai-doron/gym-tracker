package com.doron.gymtracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

data class ExerciseImages(
    val urls: List<String>
)

private val exerciseImageMap = mapOf(
    // Day A — Chest & Back
    "d1_e1" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/192/Bench-press-1.png",
        "https://wger.de/media/exercise-images/192/Bench-press-2.png"
    )),
    "d1_e2" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1127/4942b7c0-6bda-4983-88e5-86547c3d445e.png"
    )),
    "d1_e3a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/16/Incline-press-1.png",
        "https://wger.de/media/exercise-images/16/Incline-press-2.png"
    )),
    "d1_e3b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/41/Incline-bench-press-1.png",
        "https://wger.de/media/exercise-images/41/Incline-bench-press-2.png"
    )),
    "d1_e4a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1283/e7262f70-7512-408a-8d00-4c499ef632fc.jpg"
    )),
    "d1_e4b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/81/a751a438-ae2d-4751-8d61-cef0e9292174.png"
    )),
    "d1_e5a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/98/Butterfly-machine-1.png",
        "https://wger.de/media/exercise-images/98/Butterfly-machine-2.png"
    )),
    "d1_e5b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/71/Cable-crossover-1.png",
        "https://wger.de/media/exercise-images/71/Cable-crossover-2.png"
    )),
    "d1_e6a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1117/2555c4c3-a84d-47db-b83b-cbf721f12e45.png",
        "https://wger.de/media/exercise-images/1117/e74255c0-67a0-4309-b78d-2d79e6ff8c11.png"
    )),
    "d1_e6b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1117/2555c4c3-a84d-47db-b83b-cbf721f12e45.png",
        "https://wger.de/media/exercise-images/1117/e74255c0-67a0-4309-b78d-2d79e6ff8c11.png"
    )),
    "d1_e7" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/458/b7bd9c28-9f1d-4647-bd17-ab6a3adf5770.png",
        "https://wger.de/media/exercise-images/458/902e6836-394e-458b-b94e-101d714294e2.png"
    )),
    // Day B — Legs
    "d2_e1" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1801/60043328-1cfb-4289-9865-aaf64d5aaa28.jpg",
        "https://wger.de/media/exercise-images/1801/68720d5e-f422-47ac-81e4-c7b51144d302.jpg"
    )),
    "d2_e2a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/507/13d526ab-12fc-461e-828a-051dd7c13fb1.png"
    )),
    "d2_e2b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1652/0306c8c0-70cc-45d4-92de-6fa72ceaa834.webp"
    )),
    "d2_e3a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/988/6283b258-a4d7-4833-84f7-a38987022d3d.png"
    )),
    "d2_e3b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/113/Walking-lunges-1.png",
        "https://wger.de/media/exercise-images/113/Walking-lunges-2.png"
    )),
    "d2_e4" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/369/78c915d1-e46d-4d30-8124-65d68664c3ef.png",
        "https://wger.de/media/exercise-images/369/4d621b17-f6cb-4107-97c0-9f44e9a2dbc6.webp"
    )),
    "d2_e5" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/154/lying-leg-curl-machine-large-1.png",
        "https://wger.de/media/exercise-images/154/lying-leg-curl-machine-large-2.png"
    )),
    "d2_e6" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/12/4a42cc6f-648d-40cc-a72a-c49dd47e1667.webp"
    )),
    "d2_e7" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/622/9a429bd0-afd3-4ad0-8043-e9beec901c81.jpeg",
        "https://wger.de/media/exercise-images/622/d6d57067-97de-462e-a8bb-15228d730323.jpeg"
    )),
    "d2_e8a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/91/Crunches-1.png",
        "https://wger.de/media/exercise-images/91/Crunches-2.png"
    )),
    "d2_e8b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/978/d3ffe51f-7eb8-4cc9-9eae-105847af3005.png"
    )),
    // Day C — Shoulders & Arms
    "d3_e1" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/123/dumbbell-shoulder-press-large-1.png",
        "https://wger.de/media/exercise-images/123/dumbbell-shoulder-press-large-2.png"
    )),
    "d3_e2a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-1.png",
        "https://wger.de/media/exercise-images/148/lateral-dumbbell-raises-large-2.png"
    )),
    "d3_e2b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1378/7c1fcf34-fb7e-45e7-a0c1-51f296235315.jpg"
    )),
    "d3_e3a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/487/ad724e5c-b1ed-49e8-9279-a17545b0dd0b.png",
        "https://wger.de/media/exercise-images/487/692c598c-0493-42a5-be8b-b9d2e9adcd00.png"
    )),
    "d3_e3b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/822/74affc0d-03b6-4f33-b5f4-a822a2615f68.png",
        "https://wger.de/media/exercise-images/822/6a770e5d-cd62-4754-a18c-eebe2103d7c5.png"
    )),
    "d3_e4a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/74/Bicep-curls-1.png",
        "https://wger.de/media/exercise-images/74/Bicep-curls-2.png"
    )),
    "d3_e4b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/86/Bicep-hammer-curl-1.png",
        "https://wger.de/media/exercise-images/86/Bicep-hammer-curl-2.png"
    )),
    "d3_e5a" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1185/c5ca283d-8958-4fd8-9d59-a3f52a3ac66b.jpg"
    )),
    "d3_e5b" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1900/a8243245-8f8f-4e2b-93ca-694d416cb11d.png"
    )),
    "d3_e6" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/129/Standing-biceps-curl-1.png",
        "https://wger.de/media/exercise-images/129/Standing-biceps-curl-2.png"
    )),
    "d3_e7" to ExerciseImages(listOf(
        "https://wger.de/media/exercise-images/1336/ebf88217-df26-4ef7-94cb-f0c2220c6abe.webp"
    )),
    "d3_e8" to ExerciseImages(listOf(
        "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Farmers_Walk/0.jpg",
        "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Farmers_Walk/1.jpg"
    ))
)

fun getExerciseImages(exerciseId: String): ExerciseImages? = exerciseImageMap[exerciseId]

@Composable
fun ExerciseIllustration(exerciseId: String, modifier: Modifier = Modifier) {
    val images = getExerciseImages(exerciseId) ?: return

    if (images.urls.size < 2) {
        AsyncImage(
            model = images.urls.first(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        )
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            currentIndex = (currentIndex + 1) % images.urls.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = currentIndex,
            animationSpec = tween(400),
            label = "exercise-anim"
        ) { index ->
            AsyncImage(
                model = images.urls[index],
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(12.dp)
            )
        }
    }
}
