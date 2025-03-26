package com.tetris3d

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tetris3d.game.GameOptions
import com.tetris3d.game.TetrisGame
import com.tetris3d.views.NextPieceView
import com.tetris3d.views.TetrisGameView

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: TetrisGameView
    private lateinit var nextPieceView: NextPieceView
    private lateinit var scoreText: TextView
    private lateinit var linesText: TextView
    private lateinit var levelText: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var optionsButton: Button
    
    private lateinit var tetrisGame: TetrisGame
    private lateinit var gameOptions: GameOptions
    
    private var gameOverDialog: Dialog? = null
    private var optionsDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        initGameOptions()
        initGame()
        setupButtonListeners()
    }
    
    private fun initViews() {
        gameView = findViewById(R.id.tetrisGameView)
        nextPieceView = findViewById(R.id.nextPieceView)
        scoreText = findViewById(R.id.scoreText)
        linesText = findViewById(R.id.linesText)
        levelText = findViewById(R.id.levelText)
        startButton = findViewById(R.id.startButton)
        pauseButton = findViewById(R.id.pauseButton)
        optionsButton = findViewById(R.id.optionsButton)
    }
    
    private fun initGameOptions() {
        gameOptions = GameOptions(
            enable3DEffects = true,
            enableSpinAnimations = true,
            animationSpeed = 0.05f,
            startingLevel = 1
        )
        
        // Load saved options from SharedPreferences
        val prefs = getSharedPreferences("TetrisOptions", MODE_PRIVATE)
        gameOptions.enable3DEffects = prefs.getBoolean("enable3DEffects", true)
        gameOptions.enableSpinAnimations = prefs.getBoolean("enableSpinAnimations", true)
        gameOptions.animationSpeed = prefs.getFloat("animationSpeed", 0.05f)
        gameOptions.startingLevel = prefs.getInt("startingLevel", 1)
    }
    
    private fun initGame() {
        tetrisGame = TetrisGame(gameOptions)
        gameView.setGame(tetrisGame)
        nextPieceView.setGame(tetrisGame)
        
        tetrisGame.setGameStateListener(object : TetrisGame.GameStateListener {
            override fun onScoreChanged(score: Int) {
                runOnUiThread {
                    scoreText.text = score.toString()
                }
            }
            
            override fun onLinesChanged(lines: Int) {
                runOnUiThread {
                    linesText.text = lines.toString()
                }
            }
            
            override fun onLevelChanged(level: Int) {
                runOnUiThread {
                    levelText.text = level.toString()
                }
            }
            
            override fun onGameOver(finalScore: Int) {
                runOnUiThread {
                    showGameOverDialog(finalScore)
                }
            }
            
            override fun onNextPieceChanged() {
                runOnUiThread {
                    nextPieceView.invalidate()
                }
            }
        })
        
        // Start a new game automatically
        tetrisGame.startNewGame()
        updateControls()
    }
    
    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            if (tetrisGame.isGameOver) {
                tetrisGame.startNewGame()
            } else {
                tetrisGame.start()
            }
            updateControls()
        }
        
        pauseButton.setOnClickListener {
            if (tetrisGame.isRunning) {
                tetrisGame.pause()
            } else {
                tetrisGame.resume()
            }
            updateControls()
        }
        
        optionsButton.setOnClickListener {
            showOptionsDialog()
        }
    }
    
    private fun updateControls() {
        if (tetrisGame.isRunning) {
            startButton.visibility = View.GONE
            pauseButton.text = getString(R.string.pause)
        } else if (tetrisGame.isGameOver) {
            startButton.visibility = View.VISIBLE
            startButton.text = getString(R.string.start)
            pauseButton.text = getString(R.string.pause)
        } else {
            startButton.visibility = View.GONE
            pauseButton.text = getString(R.string.start)
        }
    }
    
    private fun showGameOverDialog(finalScore: Int) {
        if (gameOverDialog != null && gameOverDialog!!.isShowing) {
            gameOverDialog!!.dismiss()
        }
        
        val view = layoutInflater.inflate(R.layout.dialog_game_over, null)
        val scoreText = view.findViewById<TextView>(R.id.textFinalScore)
        val playAgainButton = view.findViewById<Button>(R.id.btnPlayAgain)
        
        scoreText.text = finalScore.toString()
        
        gameOverDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
            
        playAgainButton.setOnClickListener {
            gameOverDialog?.dismiss()
            tetrisGame.startNewGame()
            updateControls()
        }
        
        gameOverDialog?.show()
    }
    
    private fun showOptionsDialog() {
        if (optionsDialog != null && optionsDialog!!.isShowing) {
            optionsDialog!!.dismiss()
        }
        
        val view = layoutInflater.inflate(R.layout.dialog_options, null)
        
        val switch3dEffects = view.findViewById<Switch>(R.id.switch3dEffects)
        val switchSpinAnimations = view.findViewById<Switch>(R.id.switchSpinAnimations)
        val seekBarSpeed = view.findViewById<SeekBar>(R.id.seekBarAnimationSpeed)
        val numberPickerLevel = view.findViewById<NumberPicker>(R.id.numberPickerLevel)
        val btnApply = view.findViewById<Button>(R.id.btnApplyOptions)
        val btnClose = view.findViewById<Button>(R.id.btnCloseOptions)
        
        // Set up controls with current options
        switch3dEffects.isChecked = gameOptions.enable3DEffects
        switchSpinAnimations.isChecked = gameOptions.enableSpinAnimations
        
        // Convert animation speed (0.01-0.1) to progress (0-100)
        val progress = ((gameOptions.animationSpeed - 0.01f) / 0.09f * 100).toInt()
        seekBarSpeed.progress = progress
        
        // Set up level picker
        numberPickerLevel.minValue = 1
        numberPickerLevel.maxValue = 10
        numberPickerLevel.value = gameOptions.startingLevel
        
        optionsDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()
            
        btnApply.setOnClickListener {
            // Save new options
            gameOptions.enable3DEffects = switch3dEffects.isChecked
            gameOptions.enableSpinAnimations = switchSpinAnimations.isChecked
            
            // Convert progress (0-100) to animation speed (0.01-0.1)
            val animationSpeed = 0.01f + (seekBarSpeed.progress / 100f * 0.09f)
            gameOptions.animationSpeed = animationSpeed
            
            gameOptions.startingLevel = numberPickerLevel.value
            
            // Apply options to game
            tetrisGame.updateOptions(gameOptions)
            
            // Save options to SharedPreferences
            val prefs = getSharedPreferences("TetrisOptions", MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("enable3DEffects", gameOptions.enable3DEffects)
                putBoolean("enableSpinAnimations", gameOptions.enableSpinAnimations)
                putFloat("animationSpeed", gameOptions.animationSpeed)
                putInt("startingLevel", gameOptions.startingLevel)
                apply()
            }
            
            optionsDialog?.dismiss()
        }
        
        btnClose.setOnClickListener {
            optionsDialog?.dismiss()
        }
        
        optionsDialog?.show()
    }
    
    override fun onPause() {
        super.onPause()
        if (tetrisGame.isRunning) {
            tetrisGame.pause()
            updateControls()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        tetrisGame.stop()
    }
} 