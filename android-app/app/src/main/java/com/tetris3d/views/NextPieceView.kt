package com.tetris3d.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.tetris3d.game.TetrisGame

/**
 * Custom view for rendering the next Tetris piece
 */
class NextPieceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var game: TetrisGame? = null
    private val paint = Paint()
    private var blockSize = 0f
    
    // Background gradient colors
    private val bgColorStart = Color.parseColor("#1a1a2e")
    private val bgColorEnd = Color.parseColor("#0f3460")
    private lateinit var bgGradient: LinearGradient

    fun setGame(game: TetrisGame) {
        this.game = game
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Create background gradient
        bgGradient = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            bgColorStart, bgColorEnd,
            Shader.TileMode.CLAMP
        )
        
        // Determine block size based on the smaller dimension
        blockSize = (Math.min(width, height) / 4).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val game = this.game ?: return
        
        // Draw gradient background
        paint.shader = bgGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        // Draw border with glow effect
        drawBorder(canvas)
        
        // Draw the next piece
        drawNextPiece(canvas, game)
    }
    
    private fun drawBorder(canvas: Canvas) {
        // Draw a glowing border
        val borderRect = RectF(2f, 2f, width - 2f, height - 2f)
        
        // Outer glow (cyan color like in the web app)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.parseColor("#00ffff")
        paint.setShadowLayer(5f, 0f, 0f, Color.parseColor("#00ffff"))
        canvas.drawRect(borderRect, paint)
        paint.setShadowLayer(0f, 0f, 0f, 0)
    }
    
    private fun drawNextPiece(canvas: Canvas, game: TetrisGame) {
        val piece = game.getNextPieceArray()
        val color = game.getNextColor()
        
        // Center the piece in the view
        val offsetX = (width - piece[0].size * blockSize) / 2
        val offsetY = (height - piece.size * blockSize) / 2
        
        for (r in piece.indices) {
            for (c in piece[r].indices) {
                if (piece[r][c] == 1) {
                    drawBlock(canvas, offsetX + c * blockSize, offsetY + r * blockSize, color)
                }
            }
        }
    }
    
    private fun drawBlock(canvas: Canvas, x: Float, y: Float, colorStr: String) {
        val left = x
        val top = y
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
} 