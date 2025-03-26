package com.tetris3d.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.tetris3d.game.TetrisGame
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Custom view for rendering the Tetris game board
 */
class TetrisGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var game: TetrisGame? = null
    private val paint = Paint()
    private var blockSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    
    // Shadow and grid configuration
    private val showShadow = true
    private val showGrid = true
    private val showGlowEffects = true
    
    // Background gradient colors
    private val bgColorStart = Color.parseColor("#06071B") // Darker space background
    private val bgColorEnd = Color.parseColor("#0B1026") // Slightly lighter space background
    private lateinit var bgGradient: LinearGradient
    
    // Glow and star effects
    private val stars = ArrayList<Star>()
    private val random = java.util.Random()
    private val starCount = 50
    private val starColors = arrayOf(
        Color.parseColor("#FFFFFF"), // White
        Color.parseColor("#AAAAFF"), // Light blue
        Color.parseColor("#FFAAAA"), // Light red
        Color.parseColor("#AAFFAA")  // Light green
    )
    
    // Gesture detection for swipe controls
    private val gestureDetector = GestureDetector(context, TetrisGestureListener())
    
    // Define minimum swipe velocity and distance
    private val minSwipeVelocity = 30 // Lower for better responsiveness 
    private val minSwipeDistance = 15 // Lower for better responsiveness
    
    // Movement control
    private val autoRepeatHandler = Handler(Looper.getMainLooper())
    private var isAutoRepeating = false
    private var currentMovement: (() -> Unit)? = null
    private val autoRepeatDelay = 100L // Faster for smoother continuous movement
    private val initialAutoRepeatDelay = 150L // Faster initial delay
    private val interpolator = DecelerateInterpolator(1.5f)
    
    // Touch tracking for continuous swipe
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var swipeThreshold = 20f // More sensitive
    private var lastMoveTime = 0L
    private val moveCooldown = 110L // Shorter cooldown for more responsive movement
    private var tapThreshold = 10f // Slightly more forgiving tap detection
    
    // Refresh timer
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            // Update the game rotation animation
            game?.updateRotation()
            invalidate()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL)
        }
    }
    
    // Game state flags
    private var gameOver = false
    private var paused = false
    
    companion object {
        private const val REFRESH_INTERVAL = 16L // ~60fps
    }
    
    fun setGame(game: TetrisGame) {
        this.game = game
        invalidate()
        
        // Start refresh timer
        startRefreshTimer()
        
        // Update game state flags
        gameOver = game.isGameOver
        paused = !game.isRunning
    }
    
    private fun startRefreshTimer() {
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
    }
    
    private fun stopRefreshTimer() {
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Create background gradient
        bgGradient = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            bgColorStart, bgColorEnd,
            Shader.TileMode.CLAMP
        )
        
        // Calculate block size based on available space
        val rows = TetrisGame.ROWS
        val cols = TetrisGame.COLS
        
        // Determine the maximum block size that will fit in the view
        val maxBlockWidth = width.toFloat() / cols
        val maxBlockHeight = height.toFloat() / rows
        
        // Use the smaller dimension to ensure squares
        blockSize = min(maxBlockWidth, maxBlockHeight)
        
        // Center the board
        boardLeft = (width - cols * blockSize) / 2
        boardTop = (height - rows * blockSize) / 2
        
        // Initialize stars for background
        initializeStars(w, h)
    }
    
    private fun initializeStars(width: Int, height: Int) {
        stars.clear()
        for (i in 0 until starCount) {
            stars.add(Star(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height,
                size = 1f + random.nextFloat() * 2f,
                color = starColors[random.nextInt(starColors.size)],
                blinkSpeed = 0.5f + random.nextFloat() * 2f
            ))
        }
    }

    // Star class for background effect
    private data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int,
        val blinkSpeed: Float,
        var brightness: Float
    ) {
        companion object {
            private val random = java.util.Random()
        }
        
        constructor(x: Float, y: Float, size: Float, color: Int, blinkSpeed: Float) : this(
            x = x,
            y = y,
            size = size,
            color = color,
            blinkSpeed = blinkSpeed,
            brightness = random.nextFloat()
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val game = this.game ?: return
        
        // Update game state flags
        gameOver = game.isGameOver
        paused = !game.isRunning
        
        // Draw space background with gradient
        paint.shader = bgGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        // Draw stars in background
        drawStars(canvas)
        
        // Draw grid if enabled
        if (showGrid) {
            drawGrid(canvas)
        }
        
        // Draw board border with enhanced glow effect
        drawBoardBorder(canvas)
        
        // Draw the locked pieces on the board
        drawBoard(canvas, game)
        
        // Draw line clear effect if active
        if (game.isLineClearEffect()) {
            drawLineClearEffect(canvas, game)
        }
        
        // Draw shadow piece if enabled
        if (showShadow && !game.isGameOver && game.isRunning) {
            drawShadowPiece(canvas, game)
        }
        
        // Draw current active piece with 3D rotation effect
        if (!game.isGameOver) {
            drawActivePiece(canvas, game)
        }
        
        // Update animations
        if (game.isLineClearEffect()) {
            // Update line clear animation
            if (game.updateLineClear()) {
                // If line clear animation completed, invalidate again
                invalidate()
            } else {
                // Animation still in progress
                invalidate()
            }
        }
        
        // Update star animation
        updateStars()
    }
    
    private fun updateStars() {
        val currentTime = System.currentTimeMillis() / 1000f
        for (star in stars) {
            // Calculate pulsing brightness based on time and individual star speed
            star.brightness = (kotlin.math.sin(currentTime * star.blinkSpeed) + 1f) / 2f
        }
        
        // Force regular refresh to animate stars
        if (!gameOver && !paused) {
            postInvalidateDelayed(50)
        }
    }
    
    private fun drawStars(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        
        for (star in stars) {
            // Set color with alpha based on brightness
            paint.color = star.color
            paint.alpha = (255 * star.brightness).toInt()
            
            // Draw star with glow effect
            if (showGlowEffects) {
                paint.setShadowLayer(star.size * 2, 0f, 0f, star.color)
            }
            
            canvas.drawCircle(star.x, star.y, star.size * star.brightness, paint)
            
            // Reset shadow
            if (showGlowEffects) {
                paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
    }
    
    private fun drawBoardBorder(canvas: Canvas) {
        // Draw a glowing border around the game board
        val borderRect = RectF(
            boardLeft - 4f,
            boardTop - 4f,
            boardLeft + TetrisGame.COLS * blockSize + 4f,
            boardTop + TetrisGame.ROWS * blockSize + 4f
        )
        
        // Outer glow (enhanced cyan color)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#00ffff")
        
        if (showGlowEffects) {
            // Stronger glow effect
            paint.setShadowLayer(16f, 0f, 0f, Color.parseColor("#00ffff"))
        }
        
        canvas.drawRect(borderRect, paint)
        
        // Inner glow
        if (showGlowEffects) {
            paint.strokeWidth = 2f
            paint.color = Color.parseColor("#80ffff")
            paint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#80ffff"))
            
            val innerRect = RectF(
                borderRect.left + 4f,
                borderRect.top + 4f,
                borderRect.right - 4f,
                borderRect.bottom - 4f
            )
            
            canvas.drawRect(innerRect, paint)
        }
        
        // Reset shadow
        paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }
    
    private fun drawBoard(canvas: Canvas, game: TetrisGame) {
        val board = game.getBoard()
        
        for (r in 0 until TetrisGame.ROWS) {
            for (c in 0 until TetrisGame.COLS) {
                val color = board[r][c]
                if (color != TetrisGame.EMPTY) {
                    drawBlock(canvas, c, r, color)
                }
            }
        }
    }
    
    private fun drawGrid(canvas: Canvas) {
        paint.color = Color.parseColor("#333344") // Slightly blue-tinted grid
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        
        // Draw vertical lines
        for (c in 0..TetrisGame.COLS) {
            val x = boardLeft + c * blockSize
            canvas.drawLine(x, boardTop, x, boardTop + TetrisGame.ROWS * blockSize, paint)
        }
        
        // Draw horizontal lines
        for (r in 0..TetrisGame.ROWS) {
            val y = boardTop + r * blockSize
            canvas.drawLine(boardLeft, y, boardLeft + TetrisGame.COLS * blockSize, y, paint)
        }
    }
    
    private fun drawActivePiece(canvas: Canvas, game: TetrisGame) {
        val piece = game.getCurrentPieceArray()
        val x = game.getCurrentX()
        val y = game.getCurrentY()
        val color = game.getCurrentColor()
        
        // Save canvas state for rotation
        canvas.save()
        
        // Get 3D rotation values (0-3 for each axis)
        val rotationX = game.getRotation3DX()
        val rotationY = game.getRotation3DY()
        
        // Convert rotation to radians (0-2π)
        val angleX = rotationX * Math.PI / 2
        val angleY = rotationY * Math.PI / 2
        
        // Calculate center point of the piece for rotation
        val centerX = boardLeft + (x + piece[0].size / 2f) * blockSize
        val centerY = boardTop + (y + piece.size / 2f) * blockSize
        
        // Translate to center point, apply transformations, then translate back
        canvas.translate(centerX, centerY)
        
        // Apply transformations based on rotation state
        // First apply scaling to simulate flipping
        val flipX = if (rotationX.toInt() % 2 == 1) -1f else 1f
        val flipY = if (rotationY.toInt() % 2 == 1) -1f else 1f
        
        // Check if we're in the middle of an animation
        if (game.isRotating()) {
            // For animation, use perspective scaling and smooth transitions
            val scaleX = cos(angleY.toFloat()).coerceAtLeast(0.5f) * flipY
            val scaleY = cos(angleX.toFloat()).coerceAtLeast(0.5f) * flipX
            canvas.scale(scaleX, scaleY)
        } else {
            // For static display, just flip directly
            canvas.scale(flipY, flipX)
        }
        
        // Translate back
        canvas.translate(-centerX, -centerY)
        
        // Draw the piece with perspective or flip effect
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    // Calculate offset for 3D effect during animation
                    val offsetX = if (game.isRotating()) sin(angleY.toFloat()) * blockSize * 0.3f else 0f
                    val offsetY = if (game.isRotating()) sin(angleX.toFloat()) * blockSize * 0.3f else 0f
                    
                    drawBlock(
                        canvas, 
                        x + c, 
                        y + r, 
                        color,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            }
        }
        
        // Restore canvas state
        canvas.restore()
    }
    
    private fun drawShadowPiece(canvas: Canvas, game: TetrisGame) {
        val piece = game.getCurrentPieceArray()
        val x = game.getCurrentX()
        val y = game.calculateShadowY()
        
        if (y == game.getCurrentY()) {
            return  // Skip if shadow is at the same position as the piece
        }
        
        paint.color = Color.parseColor("#444444")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    val left = boardLeft + (x + c) * blockSize
                    val top = boardTop + (y + r) * blockSize
                    val right = left + blockSize
                    val bottom = top + blockSize
                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }
        }
    }
    
    private fun drawBlock(canvas: Canvas, x: Int, y: Int, colorStr: String, offsetX: Float = 0f, offsetY: Float = 0f) {
        // Skip drawing outside the board
        if (y < 0) return
        
        val left = boardLeft + x * blockSize + offsetX
        val top = boardTop + y * blockSize + offsetY
        val right = left + blockSize
        val bottom = top + blockSize
        val blockRect = RectF(left, top, right, bottom)
        
        // Parse the base color
        val baseColor = Color.parseColor(colorStr)
        
        // Create a brighter version for the glow
        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)
        val glowColor = Color.argb(255, 
            Math.min(255, red + 40),
            Math.min(255, green + 40),
            Math.min(255, blue + 40)
        )
        
        // Add glow effect
        if (showGlowEffects) {
            paint.style = Paint.Style.FILL
            paint.color = baseColor
            paint.setShadowLayer(blockSize / 4, 0f, 0f, glowColor)
        }
        
        // Draw the block fill
        paint.style = Paint.Style.FILL
        paint.color = baseColor
        canvas.drawRect(blockRect, paint)
        
        // Reset shadow
        if (showGlowEffects) {
            paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
        
        // Draw the highlight (top-left gradient)
        paint.style = Paint.Style.FILL
        val highlightPaint = Paint()
        highlightPaint.shader = LinearGradient(
            left, top, 
            right, bottom,
            Color.argb(150, 255, 255, 255), // More pronounced highlight
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(blockRect, highlightPaint)
        
        // Draw the block border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.BLACK
        canvas.drawRect(blockRect, paint)
        
        // Draw inner glow edge
        if (showGlowEffects) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            paint.color = glowColor
            
            val innerRect = RectF(
                left + 2, 
                top + 2, 
                right - 2, 
                bottom - 2
            )
            canvas.drawRect(innerRect, paint)
        }
    }
    
    // Draw line clear effect
    private fun drawLineClearEffect(canvas: Canvas, game: TetrisGame) {
        val clearedRows = game.getClearedRows()
        val progress = game.getLineClearProgress()
        
        // Different effect based on animation progress
        for (row in clearedRows) {
            for (col in 0 until TetrisGame.COLS) {
                val left = boardLeft + col * blockSize
                val top = boardTop + row * blockSize
                val right = left + blockSize
                val bottom = top + blockSize
                
                // Create a pulsing, brightening effect for cleared blocks
                val alpha = (255 * (0.5f + 0.5f * Math.sin(progress * Math.PI * 3))).toInt()
                val scale = 1.0f + 0.1f * progress
                
                // Calculate center for scaling
                val centerX = left + blockSize / 2
                val centerY = top + blockSize / 2
                
                // Save canvas state for transformation
                canvas.save()
                
                // Position at center, scale, then move back
                canvas.translate(centerX, centerY)
                canvas.scale(scale, scale)
                canvas.translate(-centerX, -centerY)
                
                // Get the color from the board
                val color = game.getBoard()[row][col]
                
                if (color != TetrisGame.EMPTY) {
                    // Draw with glow effect
                    val baseColor = Color.parseColor(color)
                    
                    // Create a brighter glow as animation progresses
                    val red = Color.red(baseColor)
                    val green = Color.green(baseColor)
                    val blue = Color.blue(baseColor)
                    
                    // Get increasingly white as effect progresses
                    val whiteBlend = progress * 0.7f
                    val newRed = (red * (1 - whiteBlend) + 255 * whiteBlend).toInt().coerceIn(0, 255)
                    val newGreen = (green * (1 - whiteBlend) + 255 * whiteBlend).toInt().coerceIn(0, 255)
                    val newBlue = (blue * (1 - whiteBlend) + 255 * whiteBlend).toInt().coerceIn(0, 255)
                    
                    val effectColor = Color.argb(alpha, newRed, newGreen, newBlue)
                    
                    // Draw with glow
                    paint.style = Paint.Style.FILL
                    paint.color = effectColor
                    
                    if (showGlowEffects) {
                        // Increase glow radius with progress
                        val glowRadius = blockSize * (0.3f + 0.7f * progress)
                        paint.setShadowLayer(glowRadius, 0f, 0f, effectColor)
                    }
                    
                    // Draw the block
                    canvas.drawRect(left, top, right, bottom, paint)
                    
                    // Add horizontal line effect
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4f * progress
                    canvas.drawLine(left, top + blockSize / 2, right, top + blockSize / 2, paint)
                    
                    // Reset shadow
                    if (showGlowEffects) {
                        paint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    }
                }
                
                // Restore canvas state
                canvas.restore()
            }
        }
    }
    
    // Handler for touch events
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver || paused) return false
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - lastTouchX
                val diffY = event.y - lastTouchY
                val currentTime = System.currentTimeMillis()
                
                // Check if cooldown has elapsed since last move
                if (currentTime - lastMoveTime < moveCooldown) {
                    return true
                }
                
                // Check if drag distance exceeds threshold for movement
                if (abs(diffX) > swipeThreshold && abs(diffX) > abs(diffY) * 1.2f) {
                    // Horizontal movement - requiring less pronounced horizontal movement for smoother control
                    if (diffX > 0) {
                        game?.moveRight()
                    } else {
                        game?.moveLeft()
                    }
                    // Update last position after processing the move
                    lastTouchX = event.x
                    lastMoveTime = currentTime
                    invalidate()
                    return true
                } else if (abs(diffY) > swipeThreshold && abs(diffY) > abs(diffX) * 1.2f) {
                    // Vertical movement - requiring less pronounced vertical movement for smoother control
                    if (diffY > 0) {
                        game?.moveDown()
                        // Update last position after processing the move
                        lastTouchY = event.y
                        lastMoveTime = currentTime
                        invalidate()
                        return true
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val diffX = event.x - lastTouchX
                val diffY = event.y - lastTouchY
                val totalMovement = abs(diffX) + abs(diffY)
                
                // If this was a tap (very minimal movement)
                if (totalMovement < tapThreshold) {
                    // Simple tap to rotate
                    game?.rotate()
                    invalidate()
                    return true
                }
                
                // Check for deliberate swipe up (hard drop) - more forgiving upward movement
                if (abs(diffY) > swipeThreshold * 1.2f && diffY < 0 && abs(diffY) > abs(diffX) * 1.5f) {
                    game?.hardDrop()
                    invalidate()
                    return true
                }
                
                stopAutoRepeat()
                return true
            }
        }
        
        return false
    }
    
    private fun startAutoRepeat(action: () -> Unit) {
        isAutoRepeating = true
        currentMovement = action
        
        val autoRepeatRunnable = object : Runnable {
            override fun run() {
                if (isAutoRepeating && currentMovement != null) {
                    currentMovement?.invoke()
                    invalidate()
                    autoRepeatHandler.postDelayed(this, autoRepeatDelay)
                }
            }
        }
        
        // Use initial delay before first repeat
        autoRepeatHandler.postDelayed(autoRepeatRunnable, initialAutoRepeatDelay)
    }
    
    private fun stopAutoRepeat() {
        isAutoRepeating = false
        currentMovement = null
        autoRepeatHandler.removeCallbacksAndMessages(null)
    }
    
    // Clean up refresh timer
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRefreshTimer()
        stopAutoRepeat()
    }
    
    // Gesture listener for swipe controls
    inner class TetrisGestureListener : GestureDetector.SimpleOnGestureListener() {
        
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
        
        // We're handling taps directly in onTouchEvent
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return false
        }
        
        override fun onFling(
            e1: MotionEvent, 
            e2: MotionEvent, 
            velocityX: Float, 
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            
            // Check if swipe is horizontal or vertical based on magnitude
            if (abs(diffX) > abs(diffY)) {
                // Horizontal swipe
                if (abs(velocityX) > minSwipeVelocity && abs(diffX) > minSwipeDistance) {
                    if (diffX > 0) {
                        // Swipe right - move right once
                        game?.moveRight()
                    } else {
                        // Swipe left - move left once
                        game?.moveLeft()
                    }
                    invalidate()
                    return true
                }
            } else {
                // Vertical swipe
                if (abs(velocityY) > minSwipeVelocity && abs(diffY) > minSwipeDistance) {
                    if (diffY > 0) {
                        // Swipe down - start soft drop
                        startAutoRepeat { game?.moveDown() }
                    } else {
                        // Swipe up - hard drop
                        game?.hardDrop()
                    }
                    invalidate()
                    return true
                }
            }
            
            return false
        }
    }

    // Create touch control buttons
    fun createTouchControlButtons() {
        // Create 3D rotation buttons
        val context = context ?: return
        
        // First check if buttons are already added to prevent duplicates
        val parent = parent as? android.view.ViewGroup ?: return
        if (parent.findViewWithTag<View>("rotate_buttons") != null) {
            return
        }
        
        // Create a container for rotation buttons
        val rotateButtons = android.widget.LinearLayout(context)
        rotateButtons.tag = "rotate_buttons"
        rotateButtons.orientation = android.widget.LinearLayout.HORIZONTAL
        rotateButtons.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Add layout to position at bottom of screen
        rotateButtons.gravity = android.view.Gravity.CENTER
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = android.view.Gravity.BOTTOM
        params.setMargins(16, 16, 16, 32) // Add more bottom margin for visibility
        rotateButtons.layoutParams = params
        
        // Create vertical flip button (X-axis rotation)
        val verticalFlipButton = android.widget.Button(context)
        verticalFlipButton.text = "Flip ↑↓"
        verticalFlipButton.tag = "flip_vertical_button"
        val buttonParams = android.widget.LinearLayout.LayoutParams(
            0,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        buttonParams.setMargins(12, 12, 12, 12)
        verticalFlipButton.layoutParams = buttonParams
        
        // Style the button
        verticalFlipButton.setBackgroundColor(android.graphics.Color.parseColor("#0088ff"))
        verticalFlipButton.setTextColor(android.graphics.Color.WHITE)
        verticalFlipButton.setPadding(8, 16, 8, 16)
        
        // Create horizontal flip button (Y-axis rotation)
        val horizontalFlipButton = android.widget.Button(context)
        horizontalFlipButton.text = "Flip ←→"
        horizontalFlipButton.tag = "flip_horizontal_button"
        horizontalFlipButton.layoutParams = buttonParams
        
        // Style the button
        horizontalFlipButton.setBackgroundColor(android.graphics.Color.parseColor("#ff5500"))
        horizontalFlipButton.setTextColor(android.graphics.Color.WHITE)
        horizontalFlipButton.setPadding(8, 16, 8, 16)
        
        // Add buttons to container
        rotateButtons.addView(verticalFlipButton)
        rotateButtons.addView(horizontalFlipButton)
        
        // Add the button container to the parent view
        val rootView = parent.rootView as? android.widget.FrameLayout
        rootView?.addView(rotateButtons)
        
        // Add click listeners
        verticalFlipButton.setOnClickListener {
            if (!game?.isGameOver!! && game?.isRunning!!) {
                game?.rotate3DX()
                verticalFlipButton.alpha = 0.7f
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    verticalFlipButton.alpha = 1.0f
                }, 150)
                invalidate()
            }
        }
        
        horizontalFlipButton.setOnClickListener {
            if (!game?.isGameOver!! && game?.isRunning!!) {
                game?.rotate3DY()
                horizontalFlipButton.alpha = 0.7f
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    horizontalFlipButton.alpha = 1.0f
                }, 150)
                invalidate()
            }
        }
        
        // Create and add instructions text view if needed
        // Note: For Android implementation we'll show a toast instead of persistent instructions
        val instructions = "Swipe to move, tap to rotate, swipe down for soft drop, swipe up for hard drop. Use buttons to flip pieces."
        android.widget.Toast.makeText(context, instructions, android.widget.Toast.LENGTH_LONG).show()
    }

    // Update the game state flags from the TetrisGame
    fun updateGameState() {
        game?.let {
            gameOver = it.isGameOver
            paused = !it.isRunning
            invalidate()
        }
    }
} 