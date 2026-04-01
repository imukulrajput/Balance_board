package com.ripplehealthcare.frst.domain.model

import androidx.annotation.DrawableRes
import com.ripplehealthcare.frst.R

data class TestInstructions(
    val id: String,
    val title: String,
    @DrawableRes val gifResId: Int,
    val steps: List<String>
)

object TestDataSource {
    private val tests = listOf(
        TestInstructions(
            id = "5_time_sit_stand",
            title = "5 Time Sit to Stand",
            gifResId = R.drawable.sts_30s,
            steps = listOf(
                "Sit in a strong chair with your back straight and feet flat on the floor.",
                "Hold your mobile phone with both hands against your chest.",
                "When you press start, stand up and sit down 5 times as quickly as possible."
            )
        ),
        TestInstructions(
            id = "30_sec_sit_to_stand",
            title = "30 Second Sit to Stand",
            gifResId = R.drawable.sts_30s,
            steps = listOf(
                "Sit in the middle of a strong chair with your back straight.",
                "Hold your mobile phone with both hands against your chest.",
                "When you press start, stand up and sit down as many times as you can in 30 seconds."
            )
        ),
        TestInstructions(
            id = "timed_up_and_go",
            title = "Timed Up and Go (TUG)",
            // Make sure to add a corresponding drawable/gif to your resources
            gifResId = R.drawable.sts_30s,
            steps = listOf(
                "Place a marker (like a bottle or tape) 3 meters (10 feet) away from your chair.",
                "Sit in the chair with your back straight and hold the phone against your chest.",
                "When you press start: Stand up, walk to the marker, turn around, walk back, and sit down."
            )
        ),
        TestInstructions(
            id = "stage_1",
            title = "Stage 1: Feet Together",
            gifResId = R.drawable.both_feet,
            steps = listOf(
                "Stand with your feet side-by-side, touching each other.",
                "Hold onto a chair or wall for support if needed initially.",
                "Once steady, let go of support. Try to hold this position for 10 seconds."
            )
        ),
        TestInstructions(
            id = "stage_2",
            title = "Stage 2: Semi-Tandem Stand",
            gifResId = R.drawable.semi_tendem_stand,
            steps = listOf(
                "Place the instep of one foot so it is touching the big toe of the other foot.",
                "Use a support to get into position.",
                "Once steady, let go and try to hold for 10 seconds."
            )
        ),
        TestInstructions(
            id = "stage_3",
            title = "Stage 3: Tandem Stand",
            gifResId = R.drawable.tandem_stand,
            steps = listOf(
                "Place one foot directly in front of the other, heel touching toe.",
                "Use a support to get into position.",
                "Once steady, let go and try to hold for 10 seconds."
            )
        ),
        TestInstructions(
            id = "stage_4",
            title = "Stage 4: One Leg Stand",
            gifResId = R.drawable.one_leg,
            steps = listOf(
                "Stand on one leg, lifting the other leg slightly off the ground.",
                "Use a support to get into position.",
                "Once steady, let go and try to hold for 10 seconds."
            )
        )
    )

    fun getTestById(id: String?): TestInstructions? {
        if (id == null) return null
        return tests.find { it.id == id }
    }
}