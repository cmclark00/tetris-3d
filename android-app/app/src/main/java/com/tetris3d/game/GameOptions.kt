package com.tetris3d.game

/**
 * Class that holds game configuration options
 */
data class GameOptions(
    var enable3DEffects: Boolean = true,
    var enableSpinAnimations: Boolean = true,
    var animationSpeed: Float = 0.05f,
    var startingLevel: Int = 1
) 