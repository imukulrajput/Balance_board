package com.ripplehealthcare.frst.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.ui.graphics.vector.ImageVector

enum class TUGPhase(val label: String, val icon: ImageVector) {
    SIT_TO_STAND("Stand Up", Icons.Default.AccessibilityNew),
    WALKING("Walk 3m & Return", Icons.Default.DirectionsWalk),
    STAND_TO_SIT("Sit Down", Icons.Default.Chair)
}