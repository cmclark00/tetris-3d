package com.tetris3d.game

import android.os.Handler
import android.os.Looper
import kotlin.random.Random

/**
 * Main class that handles Tetris game logic
 */
class TetrisGame(private var options: GameOptions) {

    companion object {
        const val ROWS = 20
        const val COLS = 10
        const val EMPTY = "black"
        
        // 3D rotation directions
        private const val ROTATION_X = 0
        private const val ROTATION_Y = 1
        
        // Refresh interval for animations
        const val REFRESH_INTERVAL = 16L // ~60fps
    }

    // Game state
    var isRunning = false
    var isGameOver = false
    private var score = 0
    private var lines = 0
    private var level = options.startingLevel
    
    // Board representation
    private val board = Array(ROWS) { Array(COLS) { EMPTY } }
    
    // Piece definitions
    private val pieces = listOf(
        // I piece - line
        listOf(
            arrayOf(
                arrayOf(0, 0, 0, 0),
                arrayOf(1, 1, 1, 1),
                arrayOf(0, 0, 0, 0),
                arrayOf(0, 0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 1, 0),
                arrayOf(0, 0, 1, 0),
                arrayOf(0, 0, 1, 0),
                arrayOf(0, 0, 1, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 0, 0),
                arrayOf(0, 0, 0, 0),
                arrayOf(1, 1, 1, 1),
                arrayOf(0, 0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 0, 0),
                arrayOf(0, 1, 0, 0),
                arrayOf(0, 1, 0, 0),
                arrayOf(0, 1, 0, 0)
            )
        ),
        // J piece
        listOf(
            arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(1, 1, 1),
                arrayOf(0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 1),
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 1, 1),
                arrayOf(0, 0, 1)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 0),
                arrayOf(1, 1, 0)
            )
        ),
        // L piece
        listOf(
            arrayOf(
                arrayOf(0, 0, 1),
                arrayOf(1, 1, 1),
                arrayOf(0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 1)
            ),
            arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 1, 1),
                arrayOf(1, 0, 0)
            ),
            arrayOf(
                arrayOf(1, 1, 0),
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 0)
            )
        ),
        // O piece - square
        listOf(
            arrayOf(
                arrayOf(0, 0, 0, 0),
                arrayOf(0, 1, 1, 0),
                arrayOf(0, 1, 1, 0),
                arrayOf(0, 0, 0, 0)
            )
        ),
        // S piece
        listOf(
            arrayOf(
                arrayOf(0, 1, 1),
                arrayOf(1, 1, 0),
                arrayOf(0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 1),
                arrayOf(0, 0, 1)
            ),
            arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(0, 1, 1),
                arrayOf(1, 1, 0)
            ),
            arrayOf(
                arrayOf(1, 0, 0),
                arrayOf(1, 1, 0),
                arrayOf(0, 1, 0)
            )
        ),
        // T piece
        listOf(
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(1, 1, 1),
                arrayOf(0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(0, 1, 1),
                arrayOf(0, 1, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 1, 1),
                arrayOf(0, 1, 0)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(1, 1, 0),
                arrayOf(0, 1, 0)
            )
        ),
        // Z piece
        listOf(
            arrayOf(
                arrayOf(1, 1, 0),
                arrayOf(0, 1, 1),
                arrayOf(0, 0, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 1),
                arrayOf(0, 1, 1),
                arrayOf(0, 1, 0)
            ),
            arrayOf(
                arrayOf(0, 0, 0),
                arrayOf(1, 1, 0),
                arrayOf(0, 1, 1)
            ),
            arrayOf(
                arrayOf(0, 1, 0),
                arrayOf(1, 1, 0),
                arrayOf(1, 0, 0)
            )
        )
    )
    
    // 3D rotation state
    private var rotation3DX = 0
    private var rotation3DY = 0
    private val maxRotation3D = 4 // Increased from typical 2 to allow for more granular rotation
    
    // 3D rotation animation
    private var isRotating = false
    private var rotationProgress = 0f
    private var targetRotationX = 0
    private var targetRotationY = 0
    private var currentRotation3DX = 0f
    private var currentRotation3DY = 0f
    
    // Piece colors
    private val colors = listOf(
        "#00FFFF", // cyan - I
        "#0000FF", // blue - J
        "#FFA500", // orange - L
        "#FFFF00", // yellow - O
        "#00FF00", // green - S
        "#800080", // purple - T
        "#FF0000"  // red - Z
    )
    
    // Current piece state
    private var currentPiece: Int = 0
    private var currentRotation: Int = 0
    private var currentX: Int = 0
    private var currentY: Int = 0
    private var currentColor: String = ""
    
    // Next piece
    private var nextPiece: Int = 0
    private var nextColor: String = ""
    
    // Random bag implementation
    private val pieceBag = mutableListOf<Int>()
    private val nextBag = mutableListOf<Int>()
    
    // Game loop
    private val gameHandler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isGameOver) {
                // Update rotation animation
                updateRotation()
                
                // If a line clear effect is in progress, wait for it to complete
                // The view will handle updating and completing the animation
                if (lineClearEffect) {
                    gameHandler.postDelayed(this, REFRESH_INTERVAL)
                    return
                }
                
                // Move the current piece down
                if (!moveDown()) {
                    // If can't move down, lock the piece
                    lockPiece()
                    clearRows()
                    
                    // If a line clear effect started, wait for next frame
                    if (lineClearEffect) {
                        gameHandler.postDelayed(this, REFRESH_INTERVAL)
                        return
                    }
                    
                    // Otherwise, continue with creating a new piece
                    if (!createNewPiece()) {
                        gameOver()
                    }
                }
                gameHandler.postDelayed(this, getDropInterval())
            }
        }
    }
    
    private var gameStateListener: GameStateListener? = null
    
    interface GameStateListener {
        fun onScoreChanged(score: Int)
        fun onLinesChanged(lines: Int)
        fun onLevelChanged(level: Int)
        fun onGameOver(finalScore: Int)
        fun onNextPieceChanged()
    }
    
    fun setGameStateListener(listener: GameStateListener) {
        this.gameStateListener = listener
    }
    
    fun start() {
        if (!isRunning) {
            isRunning = true
            isGameOver = false
            gameHandler.postDelayed(gameRunnable, getDropInterval())
        }
    }
    
    fun pause() {
        isRunning = false
        gameHandler.removeCallbacks(gameRunnable)
    }
    
    fun resume() {
        if (!isGameOver) {
            isRunning = true
            gameHandler.postDelayed(gameRunnable, getDropInterval())
        }
    }
    
    fun stop() {
        isRunning = false
        gameHandler.removeCallbacks(gameRunnable)
    }
    
    fun startNewGame() {
        // Reset game state
        isRunning = true
        isGameOver = false
        score = 0
        lines = 0
        level = options.startingLevel
        
        // Reset 3D rotation state
        rotation3DX = 0
        rotation3DY = 0
        
        // Clear the board
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                board[r][c] = EMPTY
            }
        }
        
        // Reset piece bags
        pieceBag.clear()
        nextBag.clear()
        
        // Create first piece
        generateBag()
        createNewPiece()
        
        // Update UI
        gameStateListener?.onScoreChanged(score)
        gameStateListener?.onLinesChanged(lines)
        gameStateListener?.onLevelChanged(level)
        
        // Start game loop
        gameHandler.removeCallbacks(gameRunnable)
        gameHandler.postDelayed(gameRunnable, getDropInterval())
    }
    
    fun updateOptions(options: GameOptions) {
        this.options = options
    }
    
    // Game control methods
    fun moveLeft(): Boolean {
        if (isRunning && !isGameOver) {
            if (!checkCollision(currentX - 1, currentY, getCurrentPieceArray())) {
                currentX--
                return true
            }
        }
        return false
    }
    
    fun moveRight(): Boolean {
        if (isRunning && !isGameOver) {
            if (!checkCollision(currentX + 1, currentY, getCurrentPieceArray())) {
                currentX++
                return true
            }
        }
        return false
    }
    
    fun moveDown(): Boolean {
        if (isRunning && !isGameOver) {
            if (!checkCollision(currentX, currentY + 1, getCurrentPieceArray())) {
                currentY++
                return true
            }
        }
        return false
    }
    
    fun rotate(): Boolean {
        if (isRunning && !isGameOver) {
            val nextRotation = (currentRotation + 1) % pieces[currentPiece].size
            val nextPattern = pieces[currentPiece][nextRotation]
            
            if (!checkCollision(currentX, currentY, nextPattern)) {
                currentRotation = nextRotation
                return true
            } else {
                // Try wall kicks
                // Try moving right
                if (!checkCollision(currentX + 1, currentY, nextPattern)) {
                    currentX++
                    currentRotation = nextRotation
                    return true
                }
                // Try moving left
                if (!checkCollision(currentX - 1, currentY, nextPattern)) {
                    currentX--
                    currentRotation = nextRotation
                    return true
                }
                // Try moving up (for I piece mostly)
                if (!checkCollision(currentX, currentY - 1, nextPattern)) {
                    currentY--
                    currentRotation = nextRotation
                    return true
                }
            }
        }
        return false
    }
    
    fun hardDrop(): Boolean {
        if (isRunning && !isGameOver) {
            while (moveDown()) {}
            lockPiece()
            clearRows()
            
            // If line clear animation started, return and let the game loop handle it
            if (lineClearEffect) {
                return true
            }
            
            if (!createNewPiece()) {
                gameOver()
            } else {
                // Add extra points for hard drop
                score += 2
                gameStateListener?.onScoreChanged(score)
            }
            return true
        }
        return false
    }
    
    fun rotate3DX(): Boolean {
        if (isRunning && !isGameOver && options.enable3DEffects) {
            // In 3D, rotating along X would flip the piece vertically
            rotation3DX = (rotation3DX + 1) % maxRotation3D
            
            // Start rotation animation
            if (options.enableSpinAnimations) {
                isRotating = true
                targetRotationX = rotation3DX
                rotationProgress = 0f
            }
            
            // If it's a quarter or three-quarter rotation, actually mirror the piece vertically
            if (rotation3DX % (maxRotation3D / 2) == 1) {
                // Create a vertically mirrored version of the current piece
                val currentPattern = getCurrentPieceArray()
                val rows = currentPattern.size
                val cols = if (rows > 0) currentPattern[0].size else 0
                val mirroredPattern = Array(rows) { r -> Array(cols) { c -> currentPattern[rows - 1 - r][c] } }
                
                // Check if the mirrored position is valid
                if (!checkCollision(currentX, currentY, mirroredPattern)) {
                    // Replace the current rotation with the mirrored pattern
                    // Since we don't actually modify the pieces, simulate this by finding a rotation
                    // that most closely resembles the mirrored pattern, if one exists
                    
                    // For symmetrical pieces like O, this may not change anything
                    val pieceVariants = pieces[currentPiece]
                    for (i in pieceVariants.indices) {
                        if (patternsAreEquivalent(mirroredPattern, pieceVariants[i])) {
                            currentRotation = i
                            return true
                        }
                    }
                    
                    // If no matching rotation found, just use regular rotation as fallback
                    return rotate()
                } else {
                    // Try wall kicks with the mirrored pattern
                    // Try moving right
                    if (!checkCollision(currentX + 1, currentY, mirroredPattern)) {
                        currentX++
                        // Find equivalent rotation
                        val pieceVariants = pieces[currentPiece]
                        for (i in pieceVariants.indices) {
                            if (patternsAreEquivalent(mirroredPattern, pieceVariants[i])) {
                                currentRotation = i
                                return true
                            }
                        }
                        return rotate()
                    }
                    // Try moving left
                    if (!checkCollision(currentX - 1, currentY, mirroredPattern)) {
                        currentX--
                        // Find equivalent rotation
                        val pieceVariants = pieces[currentPiece]
                        for (i in pieceVariants.indices) {
                            if (patternsAreEquivalent(mirroredPattern, pieceVariants[i])) {
                                currentRotation = i
                                return true
                            }
                        }
                        return rotate()
                    }
                    // Try moving up
                    if (!checkCollision(currentX, currentY - 1, mirroredPattern)) {
                        currentY--
                        // Find equivalent rotation
                        val pieceVariants = pieces[currentPiece]
                        for (i in pieceVariants.indices) {
                            if (patternsAreEquivalent(mirroredPattern, pieceVariants[i])) {
                                currentRotation = i
                                return true
                            }
                        }
                        return rotate()
                    }
                    
                    // If all fails, don't change the actual piece, just visual effect
                }
            }
            
            // Add extra score for 3D rotations when they don't result in a piece rotation
            score += 1
            gameStateListener?.onScoreChanged(score)
            return true
        }
        return false
    }
    
    fun rotate3DY(): Boolean {
        if (isRunning && !isGameOver && options.enable3DEffects) {
            // In 3D, rotating along Y would flip the piece horizontally
            rotation3DY = (rotation3DY + 1) % maxRotation3D
            
            // Start rotation animation
            if (options.enableSpinAnimations) {
                isRotating = true
                targetRotationY = rotation3DY
                rotationProgress = 0f
            }
            
            // If it's a quarter or three-quarter rotation, actually mirror the piece horizontally
            if (rotation3DY % (maxRotation3D / 2) == 1) {
                // Create a horizontally mirrored version of the current piece
                val currentPattern = getCurrentPieceArray()
                val rows = currentPattern.size
                val cols = if (rows > 0) currentPattern[0].size else 0
                val mirroredPattern = Array(rows) { r -> Array(cols) { c -> currentPattern[r][cols - 1 - c] } }
                
                // Try to find an equivalent pattern in any piece type
                // This allows for pieces to transform into different piece types when mirrored
                for (pieceType in 0 until pieces.size) {
                    val pieceVariants = pieces[pieceType]
                    for (rotation in pieceVariants.indices) {
                        // Check if this variant matches our mirrored pattern
                        if (patternsAreEquivalent(mirroredPattern, pieceVariants[rotation])) {
                            // Transform into this piece type with this rotation
                            currentPiece = pieceType
                            currentRotation = rotation
                            currentColor = colors[currentPiece]
                            return true
                        }
                    }
                }
                
                // If no exact match was found, find the most similar pattern
                var bestPieceType = -1
                var bestRotation = -1
                var bestScore = -1
                
                for (pieceType in 0 until pieces.size) {
                    val pieceVariants = pieces[pieceType]
                    for (rotation in pieceVariants.indices) {
                        val score = patternMatchScore(mirroredPattern, pieceVariants[rotation])
                        if (score > bestScore) {
                            bestScore = score
                            bestPieceType = pieceType
                            bestRotation = rotation
                        }
                    }
                }
                
                // If we found a reasonable match
                if (bestScore > 0) {
                    // If this is a collision-free position, perform the transformation
                    if (!checkCollision(currentX, currentY, pieces[bestPieceType][bestRotation])) {
                        currentPiece = bestPieceType
                        currentRotation = bestRotation
                        currentColor = colors[currentPiece]
                        return true
                    } else {
                        // Try wall kicks with the new piece
                        // Try moving right
                        if (!checkCollision(currentX + 1, currentY, pieces[bestPieceType][bestRotation])) {
                            currentX++
                            currentPiece = bestPieceType
                            currentRotation = bestRotation
                            currentColor = colors[currentPiece]
                            return true
                        }
                        // Try moving left
                        if (!checkCollision(currentX - 1, currentY, pieces[bestPieceType][bestRotation])) {
                            currentX--
                            currentPiece = bestPieceType
                            currentRotation = bestRotation
                            currentColor = colors[currentPiece]
                            return true
                        }
                        // Try moving up
                        if (!checkCollision(currentX, currentY - 1, pieces[bestPieceType][bestRotation])) {
                            currentY--
                            currentPiece = bestPieceType
                            currentRotation = bestRotation
                            currentColor = colors[currentPiece]
                            return true
                        }
                    }
                }
                
                // If we couldn't find a good transformation, just do a regular rotation
                // as a fallback to ensure some response to the user's action
                return rotate()
            }
            
            // Add extra score for 3D rotations when they don't result in a piece rotation
            score += 1
            gameStateListener?.onScoreChanged(score)
            return true
        }
        return false
    }
    
    // Method to update rotation animation
    fun updateRotation() {
        if (isRotating && options.enableSpinAnimations) {
            rotationProgress += options.animationSpeed
            if (rotationProgress >= 1f) {
                // Animation complete
                rotationProgress = 1f
                isRotating = false
                currentRotation3DX = targetRotationX.toFloat()
                currentRotation3DY = targetRotationY.toFloat()
            } else {
                // Smooth interpolation for rotation
                currentRotation3DX = rotation3DX * rotationProgress + 
                    (rotation3DX - 1 + maxRotation3D) % maxRotation3D * (1f - rotationProgress)
                currentRotation3DY = rotation3DY * rotationProgress + 
                    (rotation3DY - 1 + maxRotation3D) % maxRotation3D * (1f - rotationProgress)
            }
        }
    }
    
    private fun generateBag() {
        if (pieceBag.isEmpty()) {
            // If both bags are empty, initialize both
            if (nextBag.isEmpty()) {
                // Fill the next bag with 0-6 (all 7 pieces) in random order
                val tempBag = (0..6).toMutableList()
                tempBag.shuffle()
                nextBag.addAll(tempBag)
            }
            // Move the next bag to current and create a new next bag
            pieceBag.addAll(nextBag)
            nextBag.clear()
            
            // Fill the next bag again
            val tempBag = (0..6).toMutableList()
            tempBag.shuffle()
            nextBag.addAll(tempBag)
        }
    }
    
    private fun getNextPieceFromBag(): Int {
        if (pieceBag.isEmpty()) {
            generateBag()
        }
        return pieceBag.removeAt(0)
    }
    
    private fun createNewPiece(): Boolean {
        // Get next piece from bag
        currentPiece = nextPiece
        currentColor = nextColor
        
        // Generate next piece
        nextPiece = getNextPieceFromBag()
        nextColor = colors[nextPiece]
        
        // If it's the first piece, generate the current one too
        if (currentColor.isEmpty()) {
            currentPiece = getNextPieceFromBag()
            currentColor = colors[currentPiece]
        }
        
        // Reset position and rotation
        currentRotation = 0
        currentX = COLS / 2 - 2
        currentY = 0
        
        // Notify next piece changed
        gameStateListener?.onNextPieceChanged()
        
        // Check if game over (collision at starting position)
        return !checkCollision(currentX, currentY, getCurrentPieceArray())
    }
    
    private fun lockPiece() {
        val piece = getCurrentPieceArray()
        
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    val boardRow = currentY + r
                    val boardCol = currentX + c
                    
                    if (boardRow >= 0 && boardRow < ROWS && boardCol >= 0 && boardCol < COLS) {
                        board[boardRow][boardCol] = currentColor
                    }
                }
            }
        }
    }
    
    // Line clearing animation properties
    private var lineClearEffect = false
    private var clearedRows = mutableListOf<Int>()
    private var lineClearProgress = 0f
    private val maxLineClearDuration = 0.5f  // In seconds
    private var lineClearStartTime = 0L
    
    // Getter for line clear effect
    fun isLineClearEffect(): Boolean = lineClearEffect
    
    // Get the cleared rows for animation
    fun getClearedRows(): List<Int> = clearedRows
    
    // Get line clear animation progress (0-1)
    fun getLineClearProgress(): Float = lineClearProgress
    
    private fun clearRows() {
        clearedRows.clear()
        
        // First pass: identify full rows
        for (r in 0 until ROWS) {
            var rowFull = true
            
            for (c in 0 until COLS) {
                if (board[r][c] == EMPTY) {
                    rowFull = false
                    break
                }
            }
            
            if (rowFull) {
                clearedRows.add(r)
            }
        }
        
        // If we have cleared rows, start the animation
        if (clearedRows.isNotEmpty()) {
            lineClearEffect = true
            lineClearProgress = 0f
            lineClearStartTime = System.currentTimeMillis()
            
            // The actual row clearing will be done when the animation completes
            // This is handled in the updateLineClear method
            
            // The rows are still part of the board during animation but will be
            // displayed with a special effect by the view
        } else {
            // No rows to clear, continue with normal gameplay
            return
        }
        
        // Mark the start of the animation
        // The actual row clearance will happen after the animation
    }
    
    // Update line clear animation
    fun updateLineClear(): Boolean {
        if (!lineClearEffect) return false
        
        // Calculate progress based on elapsed time
        val elapsedTime = (System.currentTimeMillis() - lineClearStartTime) / 1000f
        lineClearProgress = (elapsedTime / maxLineClearDuration).coerceIn(0f, 1f)
        
        // If animation is complete, apply the row clearing
        if (lineClearProgress >= 1f) {
            // Actually clear the rows and update score
            completeLineClear()
            
            // Reset animation state
            lineClearEffect = false
            lineClearProgress = 0f
            return true
        }
        
        return false
    }
    
    // Complete the line clearing after animation
    private fun completeLineClear() {
        val linesCleared = clearedRows.size
        
        // Process cleared rows in descending order to avoid index issues
        val sortedRows = clearedRows.sortedDescending()
        
        for (row in sortedRows) {
            // Move all rows above down
            for (y in row downTo 1) {
                for (c in 0 until COLS) {
                    board[y][c] = board[y - 1][c]
                }
            }
            
            // Clear top row
            for (c in 0 until COLS) {
                board[0][c] = EMPTY
            }
        }
        
        // Update lines and score
        lines += linesCleared
        
        // Calculate score based on lines cleared and level
        when (linesCleared) {
            1 -> score += 100 * level
            2 -> score += 300 * level
            3 -> score += 500 * level
            4 -> score += 800 * level
        }
        
        // Update level (every 10 lines)
        level = (lines / 10) + options.startingLevel
        
        // Notify listeners
        gameStateListener?.onScoreChanged(score)
        gameStateListener?.onLinesChanged(lines)
        gameStateListener?.onLevelChanged(level)
    }
    
    private fun checkCollision(x: Int, y: Int, piece: Array<Array<Int>>): Boolean {
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    val boardRow = y + r
                    val boardCol = x + c
                    
                    // Check boundaries
                    if (boardCol < 0 || boardCol >= COLS || boardRow >= ROWS) {
                        return true
                    }
                    
                    // Skip check above the board
                    if (boardRow < 0) continue
                    
                    // Check if position already filled
                    if (board[boardRow][boardCol] != EMPTY) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    private fun gameOver() {
        isRunning = false
        isGameOver = true
        gameHandler.removeCallbacks(gameRunnable)
        gameStateListener?.onGameOver(score)
    }
    
    private fun getDropInterval(): Long {
        // Speed increases with level
        return (1000 * Math.pow(0.8, (level - 1).toDouble())).toLong()
    }
    
    // Getters for rendering
    fun getBoard(): Array<Array<String>> {
        return board
    }
    
    fun getCurrentPiece(): Int {
        return currentPiece
    }
    
    fun getCurrentRotation(): Int {
        return currentRotation
    }
    
    fun getCurrentX(): Int {
        return currentX
    }
    
    fun getCurrentY(): Int {
        return currentY
    }
    
    fun getCurrentColor(): String {
        return currentColor
    }
    
    fun getNextPiece(): Int {
        return nextPiece
    }
    
    fun getNextColor(): String {
        return nextColor
    }
    
    fun getCurrentPieceArray(): Array<Array<Int>> {
        return pieces[currentPiece][currentRotation]
    }
    
    fun getNextPieceArray(): Array<Array<Int>> {
        return pieces[nextPiece][0]
    }
    
    fun calculateShadowY(): Int {
        var shadowY = currentY
        
        while (!checkCollision(currentX, shadowY + 1, getCurrentPieceArray())) {
            shadowY++
        }
        
        return shadowY
    }
    
    // Get current rotation values for rendering
    fun getRotation3DX(): Float = if (isRotating) currentRotation3DX else rotation3DX.toFloat()
    fun getRotation3DY(): Float = if (isRotating) currentRotation3DY else rotation3DY.toFloat()
    fun isRotating(): Boolean = isRotating
    
    // Helper function to check if two patterns are equivalent (ignoring empty space)
    private fun patternsAreEquivalent(pattern1: Array<Array<Int>>, pattern2: Array<Array<Int>>): Boolean {
        // Quick check for size match
        if (pattern1.size != pattern2.size) return false
        if (pattern1.isEmpty() || pattern2.isEmpty()) return pattern1.isEmpty() && pattern2.isEmpty()
        if (pattern1[0].size != pattern2[0].size) return false
        
        // Check if cells with 1s match in both patterns
        for (r in pattern1.indices) {
            for (c in pattern1[r].indices) {
                if (pattern1[r][c] != pattern2[r][c]) {
                    return false
                }
            }
        }
        
        return true
    }
    
    // Helper function to score how well two patterns match (higher score = better match)
    private fun patternMatchScore(pattern1: Array<Array<Int>>, pattern2: Array<Array<Int>>): Int {
        // Quick check for size match
        if (pattern1.size != pattern2.size) return 0
        if (pattern1.isEmpty() || pattern2.isEmpty()) return if (pattern1.isEmpty() && pattern2.isEmpty()) 1 else 0
        if (pattern1[0].size != pattern2[0].size) return 0
        
        // Count matching cells
        var matchCount = 0
        for (r in pattern1.indices) {
            for (c in pattern1[r].indices) {
                if (pattern1[r][c] == pattern2[r][c]) {
                    matchCount++
                }
            }
        }
        
        return matchCount
    }
} 