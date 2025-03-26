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
    
    // Background gradient colors
    private val bgColorStart = Color.parseColor("#1a1a2e")
    private val bgColorEnd = Color.parseColor("#0f3460")
    private lateinit var bgGradient: LinearGradient
    
    // Gesture detection for swipe controls
    private val gestureDetector = GestureDetector(context, TetrisGestureListener())
    
    // Define minimum swipe velocity and distance
    private val minSwipeVelocity = 50 // Lowered for better sensitivity
    private val minSwipeDistance = 20 // Lowered for better sensitivity
    
    // Movement control
    private val autoRepeatHandler = Handler(Looper.getMainLooper())
    private var isAutoRepeating = false
    private var currentMovement: (() -> Unit)? = null
    private val autoRepeatDelay = 40L // Faster repeat for smoother movement
    private val initialAutoRepeatDelay = 100L // Initial delay before repeating
    private val interpolator = DecelerateInterpolator(1.5f)
    
    // Touch tracking for continuous swipe
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var swipeThreshold = 15f // Distance needed to trigger a move while dragging
    
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
    
    companion object {
        private const val REFRESH_INTERVAL = 16L // ~60fps
    }
    
    fun setGame(game: TetrisGame) {
        this.game = game
        invalidate()
        
        // Start refresh timer
        startRefreshTimer()
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
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val game = this.game ?: return
        
        // Draw gradient background
        paint.shader = bgGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        // Draw grid if enabled
        if (showGrid) {
            drawGrid(canvas)
        }
        
        // Draw board border with glow effect
        drawBoardBorder(canvas)
        
        // Draw the locked pieces on the board
        drawBoard(canvas, game)
        
        // Draw shadow piece if enabled
        if (showShadow && !game.isGameOver && game.isRunning) {
            drawShadowPiece(canvas, game)
        }
        
        // Draw current active piece with 3D rotation effect
        if (!game.isGameOver) {
            drawActivePiece(canvas, game)
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
        
        // Outer glow (cyan color like in the web app)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#00ffff")
        paint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#00ffff"))
        canvas.drawRect(borderRect, paint)
        paint.setShadowLayer(0f, 0f, 0f, 0)
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
        paint.color = Color.parseColor("#222222")
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
        
        // Translate to center point, rotate, then translate back
        canvas.translate(centerX, centerY)
        
        // Apply 3D perspective scaling based on rotation angles
        val scaleX = cos(angleY.toFloat()).coerceAtLeast(0.5f)
        val scaleY = cos(angleX.toFloat()).coerceAtLeast(0.5f)
        canvas.scale(scaleX, scaleY)
        
        // Translate back
        canvas.translate(-centerX, -centerY)
        
        // Draw the piece with perspective
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    // Calculate position with perspective effect
                    val offsetX = sin(angleY.toFloat()) * blockSize * 0.2f
                    val offsetY = sin(angleX.toFloat()) * blockSize * 0.2f
                    
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
        
        // Draw the block fill
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor(colorStr)
        canvas.drawRect(blockRect, paint)
        
        // Draw the highlight (top-left gradient)
        paint.style = Paint.Style.FILL
        val highlightPaint = Paint()
        highlightPaint.shader = LinearGradient(
            left, top, 
            right, bottom,
            Color.argb(120, 255, 255, 255), 
            Color.argb(0, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(blockRect, highlightPaint)
        
        // Draw the block border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.BLACK
        canvas.drawRect(blockRect, paint)
    }
    
    // Handler for touch events
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val diffX = event.x - lastTouchX
                val diffY = event.y - lastTouchY
                
                // Check if drag distance exceeds threshold for continuous movement
                if (abs(diffX) > swipeThreshold && abs(diffX) > abs(diffY)) {
                    // Horizontal continuous movement
                    if (diffX > 0) {
                        game?.moveRight()
                    } else {
                        game?.moveLeft()
                    }
                    // Update last position after processing the move
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                } else if (abs(diffY) > swipeThreshold && abs(diffY) > abs(diffX)) {
                    // Vertical continuous movement - only for downward
                    if (diffY > 0) {
                        game?.moveDown()
                        // Update last position after processing the move
                        lastTouchX = event.x
                        lastTouchY = event.y
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                stopAutoRepeat()
            }
        }
        
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
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
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Determine if tap is on left or right side of screen
            val screenMiddle = width / 2
            
            if (e.x < screenMiddle) {
                // Left side - rotate counterclockwise (in a real 3D game)
                game?.rotate3DX()
            } else {
                // Right side - rotate clockwise
                game?.rotate3DY()
            }
            
            invalidate()
            return true
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
                        // Swipe right - use auto-repeat for smoother movement
                        startAutoRepeat { game?.moveRight() }
                    } else {
                        // Swipe left - use auto-repeat for smoother movement
                        startAutoRepeat { game?.moveLeft() }
                    }
                    return true
                }
            } else {
                // Vertical swipe
                if (abs(velocityY) > minSwipeVelocity && abs(diffY) > minSwipeDistance) {
                    if (diffY > 0) {
                        // Swipe down - start soft drop with auto-repeat
                        startAutoRepeat { game?.moveDown() }
                    } else {
                        // Swipe up - hard drop
                        game?.hardDrop()
                        invalidate()
                    }
                    return true
                }
            }
            
            return false
        }
    }
} 