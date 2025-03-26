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
                
                // Move the current piece down
                if (!moveDown()) {
                    // If can't move down, lock the piece
                    lockPiece()
                    clearRows()
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
            // In 3D, rotating along X would change the way the piece appears from front/back
            rotation3DX = (rotation3DX + 1) % maxRotation3D
            
            // Start rotation animation
            if (options.enableSpinAnimations) {
                isRotating = true
                targetRotationX = rotation3DX
                rotationProgress = 0f
            }
            
            // If it's a quarter or three-quarter rotation, actually change the piece orientation
            if (rotation3DX % (maxRotation3D / 2) == 1) {
                // This simulates a 3D rotation by performing a 2D rotation
                return rotate()
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
            // In 3D, rotating along Y would change the way the piece appears from left/right
            rotation3DY = (rotation3DY + 1) % maxRotation3D
            
            // Start rotation animation
            if (options.enableSpinAnimations) {
                isRotating = true
                targetRotationY = rotation3DY
                rotationProgress = 0f
            }
            
            // If it's a quarter or three-quarter rotation, actually change the piece orientation
            if (rotation3DY % (maxRotation3D / 2) == 1) {
                // This simulates a 3D rotation by performing a 2D rotation in the opposite direction
                val nextRotation = (currentRotation - 1 + pieces[currentPiece].size) % pieces[currentPiece].size
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
    
    private fun clearRows() {
        var linesCleared = 0
        
        for (r in 0 until ROWS) {
            var rowFull = true
            
            for (c in 0 until COLS) {
                if (board[r][c] == EMPTY) {
                    rowFull = false
                    break
                }
            }
            
            if (rowFull) {
                // Move all rows above down
                for (y in r downTo 1) {
                    for (c in 0 until COLS) {
                        board[y][c] = board[y - 1][c]
                    }
                }
                
                // Clear top row
                for (c in 0 until COLS) {
                    board[0][c] = EMPTY
                }
                
                linesCleared++
            }
        }
        
        if (linesCleared > 0) {
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
} 