// Get canvas and context
const canvas = document.getElementById('tetris');
const ctx = canvas.getContext('2d');
const nextPieceCanvas = document.getElementById('next-piece');
const nextPieceCtx = nextPieceCanvas.getContext('2d');
const ROWS = 20;
const COLS = 10;
const BLOCK_SIZE = 30;
const EMPTY = 'black';
const PREVIEW_BLOCK_SIZE = 25;

// Mobile detection
const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
let touchControls = false;

// 7-bag randomization variables
let pieceBag = [];
let nextBag = [];

// Set canvas dimensions to match game board
canvas.width = COLS * BLOCK_SIZE;
canvas.height = ROWS * BLOCK_SIZE;

// Score elements
const scoreElement = document.getElementById('score');
const levelElement = document.getElementById('level');
const linesElement = document.getElementById('lines');
const finalScoreElement = document.getElementById('final-score');
const gameOverModal = document.getElementById('game-over-modal');

// Buttons
const startBtn = document.getElementById('start-btn');
const pauseBtn = document.getElementById('pause-btn');
const playAgainBtn = document.getElementById('play-again-btn');
const shadowBtn = document.getElementById('shadow-btn');
const optionsBtn = document.getElementById('options-btn');
const optionsCloseBtn = document.getElementById('options-close-btn');

// Options modal and controls
const optionsModal = document.getElementById('options-modal');
const toggle3DEffects = document.getElementById('toggle-3d-effects');
const toggleSpinAnimations = document.getElementById('toggle-spin-animations');
const animationSpeedSlider = document.getElementById('animation-speed');
const toggleMobileControls = document.getElementById('toggle-mobile-controls');

// Controller elements
const controllerStatus = document.getElementById('controller-status');

// Audio elements
const moveSound = document.getElementById('move-sound');
const rotateSound = document.getElementById('rotate-sound');
const dropSound = document.getElementById('drop-sound');

// Game variables
let score = 0;
let level = 1;
let lines = 0;
let gameOver = false;
let paused = false;
let dropStart;
let gameInterval;
let nextPiece;
let showShadow = true; // Toggle shadow display

// Game options
let enable3DEffects = true;
let enableSpinAnimations = true;
let animationSpeed = 0.05;
let forceMobileControls = false;

// Controller variables
let gamepadConnected = false;
let controllers = {};
let controllerMapping = {
    left: [14, 'dpadLeft'],     // D-pad left or left stick left
    right: [15, 'dpadRight'],   // D-pad right or left stick right
    down: [13, 'dpadDown'],     // D-pad down or left stick down
    rotateLeft: [0, 'buttonA'], // A button
    rotateRight: [1, 'buttonB'], // B button
    mirrorH: [3, 'buttonY'],    // Y button - horizontal mirror
    mirrorV: [2, 'buttonX'],    // X button - vertical mirror
    hardDrop: [7, 'rightTrigger'], // RT button
    pause: [9, 'start']         // Start button
};
let lastControllerState = {};
let controllerPollingRate = 100; // ms
let controllerInterval;

// Fireworks array
let fireworks = [];

// Create the board
const board = Array.from({ length: ROWS }, () => Array(COLS).fill(EMPTY));

// Define 3D-enabled tetris pieces with different orientations
const PIECES = [
    // I piece - line
    [
        [
            [0, 0, 0, 0],
            [1, 1, 1, 1],
            [0, 0, 0, 0],
            [0, 0, 0, 0]
        ],
        [
            [0, 0, 1, 0],
            [0, 0, 1, 0],
            [0, 0, 1, 0],
            [0, 0, 1, 0]
        ],
        [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [1, 1, 1, 1],
            [0, 0, 0, 0]
        ],
        [
            [0, 1, 0, 0],
            [0, 1, 0, 0],
            [0, 1, 0, 0],
            [0, 1, 0, 0]
        ]
    ],
    // J piece
    [
        [
            [1, 0, 0],
            [1, 1, 1],
            [0, 0, 0]
        ],
        [
            [0, 1, 1],
            [0, 1, 0],
            [0, 1, 0]
        ],
        [
            [0, 0, 0],
            [1, 1, 1],
            [0, 0, 1]
        ],
        [
            [0, 1, 0],
            [0, 1, 0],
            [1, 1, 0]
        ]
    ],
    // L piece
    [
        [
            [0, 0, 1],
            [1, 1, 1],
            [0, 0, 0]
        ],
        [
            [0, 1, 0],
            [0, 1, 0],
            [0, 1, 1]
        ],
        [
            [0, 0, 0],
            [1, 1, 1],
            [1, 0, 0]
        ],
        [
            [1, 1, 0],
            [0, 1, 0],
            [0, 1, 0]
        ]
    ],
    // O piece - square
    [
        [
            [0, 0, 0, 0],
            [0, 1, 1, 0],
            [0, 1, 1, 0],
            [0, 0, 0, 0]
        ]
    ],
    // S piece
    [
        [
            [0, 1, 1],
            [1, 1, 0],
            [0, 0, 0]
        ],
        [
            [0, 1, 0],
            [0, 1, 1],
            [0, 0, 1]
        ],
        [
            [0, 0, 0],
            [0, 1, 1],
            [1, 1, 0]
        ],
        [
            [1, 0, 0],
            [1, 1, 0],
            [0, 1, 0]
        ]
    ],
    // T piece
    [
        [
            [0, 1, 0],
            [1, 1, 1],
            [0, 0, 0]
        ],
        [
            [0, 1, 0],
            [0, 1, 1],
            [0, 1, 0]
        ],
        [
            [0, 0, 0],
            [1, 1, 1],
            [0, 1, 0]
        ],
        [
            [0, 1, 0],
            [1, 1, 0],
            [0, 1, 0]
        ]
    ],
    // Z piece
    [
        [
            [1, 1, 0],
            [0, 1, 1],
            [0, 0, 0]
        ],
        [
            [0, 0, 1],
            [0, 1, 1],
            [0, 1, 0]
        ],
        [
            [0, 0, 0],
            [1, 1, 0],
            [0, 1, 1]
        ],
        [
            [0, 1, 0],
            [1, 1, 0],
            [1, 0, 0]
        ]
    ]
];

// Colors for pieces
const COLORS = [
    'cyan', 'blue', 'orange', 'yellow', 'green', 'purple', 'red'
];

// Set up gradient colors for a more vibrant look
const GRADIENT_COLORS = {
    'cyan': ['#00FFFF', '#00CCFF'],
    'blue': ['#0000FF', '#0000CC'],
    'orange': ['#FFA500', '#FF8C00'],
    'yellow': ['#FFFF00', '#FFCC00'],
    'green': ['#00FF00', '#00CC00'],
    'purple': ['#800080', '#660066'],
    'red': ['#FF0000', '#CC0000']
};

// 3D extension - additional orientations for vertical and horizontal rotations
const PIECE_3D_ORIENTATIONS = {
    // Each key represents a piece, value is array of matrices for different 3D orientations
    0: [], // I piece
    1: [], // J piece
    2: [], // L piece 
    3: [], // O piece
    4: [], // S piece
    5: [], // T piece
    6: []  // Z piece
};

// Initialize additional 3D orientations (for demonstration purposes)
// In a full implementation, you would define these completely based on 3D math
for (let i = 0; i < PIECES.length; i++) {
    // Generate a few placeholder 3D rotations
    // Note: These are simplified for demo purposes
    const baseShape = PIECES[i][0];
    const rows = baseShape.length;
    const cols = baseShape[0].length;
    
    // Create horizontal rotation (example)
    let horizontalRotation = Array(rows).fill().map(() => Array(cols).fill(0));
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            if (baseShape[r][c]) {
                // Simple transformation - just example
                horizontalRotation[rows - 1 - r][c] = 1;
            }
        }
    }
    
    // Create vertical rotation (example)
    let verticalRotation = Array(rows).fill().map(() => Array(cols).fill(0));
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            if (baseShape[r][c]) {
                // Simple transformation - just example
                verticalRotation[r][cols - 1 - c] = 1;
            }
        }
    }
    
    // Add these rotations to our 3D orientations
    PIECE_3D_ORIENTATIONS[i].push(horizontalRotation);
    PIECE_3D_ORIENTATIONS[i].push(verticalRotation);
}

// Firework class for visual effects
class Firework {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.particles = [];
        this.particleCount = 50;
        this.gravity = 0.2;
        this.isDone = false;
        this.colors = ['#FF0000', '#00FF00', '#0000FF', '#FFFF00', '#FF00FF', '#00FFFF'];
        
        // Create particles
        for (let i = 0; i < this.particleCount; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = Math.random() * 3 + 2;
            const size = Math.random() * 3 + 1;
            const color = this.colors[Math.floor(Math.random() * this.colors.length)];
            
            this.particles.push({
                x: this.x,
                y: this.y,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed,
                size: size,
                color: color,
                alpha: 1
            });
        }
    }
    
    update() {
        let allDone = true;
        
        for (let i = 0; i < this.particles.length; i++) {
            const p = this.particles[i];
            
            // Update position
            p.x += p.vx;
            p.y += p.vy;
            
            // Apply gravity
            p.vy += this.gravity;
            
            // Reduce alpha (fade out)
            p.alpha -= 0.01;
            
            if (p.alpha > 0) {
                allDone = false;
            }
        }
        
        this.isDone = allDone;
    }
    
    draw() {
        for (let i = 0; i < this.particles.length; i++) {
            const p = this.particles[i];
            
            if (p.alpha <= 0) continue;
            
            ctx.globalAlpha = p.alpha;
            ctx.fillStyle = p.color;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.closePath();
            ctx.fill();
        }
        
        ctx.globalAlpha = 1; // Reset alpha
    }
}

// The Piece class
class Piece {
    constructor(tetromino, tetrominoN, color) {
        this.tetromino = tetromino;
        this.color = color;
        
        this.tetrominoN = tetrominoN || 0; // Rotation state
        this.activeTetromino = this.tetromino[this.tetrominoN];
        this.shadowTetromino = this.activeTetromino; // For shadow calculation
        
        // Starting position
        this.x = 3;
        this.y = -2;
        
        // Shadow position
        this.shadowY = 0;
        this.calculateShadowY();
        
        // 3D rotation properties
        this.rotationAngleX = 0; // For vertical rotation
        this.rotationAngleY = 0; // For horizontal rotation
        this.rotationAngleZ = 0; // For standard rotation
        this.rotationTransition = false;
        this.rotationDirection = null;
        this.rotationProgress = 0;
        this.rotationSpeed = animationSpeed; // Speed of rotation animation
        this.rotationEasing = true; // Use easing for smoother animation
        this.showCompletionEffect = false;
        this.completionEffectProgress = 0;
    }
    
    // Draw the piece
    draw() {
        // Store current position before drawing for next clear
        previousPiecePosition = {
            x: this.x,
            y: this.y,
            shape: this.activeTetromino,
            exists: true
        };
        
        if (this.rotationTransition) {
            // Draw with 3D rotation effect
            this.draw3D();
        } else if (this.showCompletionEffect) {
            // Draw completion effect
            this.drawCompletionEffect();
        } else {
            // Draw shadow if enabled
            if (showShadow) {
                this.drawShadow();
            }
            
            // Draw regular 2D
            for (let r = 0; r < this.activeTetromino.length; r++) {
                for (let c = 0; c < this.activeTetromino[r].length; c++) {
                    if (this.activeTetromino[r][c]) {
                        drawSquare(this.x + c, this.y + r, this.color);
                    }
                }
            }
        }
    }
    
    // Draw the shadow piece - always use the 2D representation for shadow
    drawShadow() {
        this.calculateShadowY();
        
        // Only draw shadow if it's inside the board boundaries
        if (this.shadowY >= 0 && this.shadowY < ROWS) {
            // Use the 2D shadow tetromino for drawing the shadow
            for (let r = 0; r < this.shadowTetromino.length; r++) {
                for (let c = 0; c < this.shadowTetromino[r].length; c++) {
                    if (this.shadowTetromino[r][c]) {
                        // Only draw if inside the playable area
                        if (this.shadowY + r >= 0 && this.shadowY + r < ROWS && 
                            this.x + c >= 0 && this.x + c < COLS) {
                            drawShadowSquare(this.x + c, this.shadowY + r);
                        }
                    }
                }
            }
        }
    }
    
    // Calculate where the shadow should be drawn using the 2D representation
    calculateShadowY() {
        let testY = this.y;
        
        // Find where the piece would land using shadowTetromino (2D shape)
        while (testY < ROWS && !this.collision(0, testY - this.y, this.shadowTetromino)) {
            testY++;
        }
        
        // Back up one step since we found collision
        this.shadowY = testY - 1;
    }
    
    // Draw the next piece in preview area
    drawNextPiece() {
        nextPieceCtx.clearRect(0, 0, nextPieceCanvas.width, nextPieceCanvas.height);
        
        const pieceHeight = this.activeTetromino.length;
        const pieceWidth = this.activeTetromino[0].length;
        
        // Center the piece in the preview
        const offsetX = Math.floor((nextPieceCanvas.width / PREVIEW_BLOCK_SIZE - pieceWidth) / 2);
        const offsetY = Math.floor((nextPieceCanvas.height / PREVIEW_BLOCK_SIZE - pieceHeight) / 2);
        
        for (let r = 0; r < pieceHeight; r++) {
            for (let c = 0; c < pieceWidth; c++) {
                if (this.activeTetromino[r][c]) {
                    drawPreviewSquare(offsetX + c, offsetY + r, this.color);
                }
            }
        }
    }
    
    // Undraw the piece more thoroughly to prevent duplicates
    undraw() {
        // Clear the area around the piece to ensure all traces are removed
        const padding = 1; // Extra padding to ensure we clear any artifacts
        
        // Calculate bounds with existing tetromino
        const minX = Math.max(0, this.x - padding);
        const maxX = Math.min(COLS - 1, this.x + this.activeTetromino[0].length + padding);
        const minY = Math.max(0, this.y - padding);
        const maxY = Math.min(ROWS - 1, this.y + this.activeTetromino.length + padding);
        
        // Clear the area
        for (let r = minY; r <= maxY; r++) {
            for (let c = minX; c <= maxX; c++) {
                // Only clear if it's not a locked piece on the board
                if (r >= 0 && c >= 0 && r < ROWS && c < COLS && board[r][c] === EMPTY) {
                    drawSquare(c, r, EMPTY);
                }
            }
        }
    }
    
    // Move down
    moveDown() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Try to move down by 1
        if (!this.collision(0, 1, this.activeTetromino)) {
            this.y++;
        } else {
            // If can't move down, lock the piece
            this.lock();
            p = randomPiece();
            p.calculateShadowY(); // Calculate shadow for new piece
        }
        
        this.draw();
    }
    
    // Move right
    moveRight() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Try to move right
        if (!this.collision(1, 0, this.activeTetromino)) {
            this.x++;
        }
        
        this.calculateShadowY();
        this.draw();
    }
    
    // Move left
    moveLeft() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Try to move left
        if (!this.collision(-1, 0, this.activeTetromino)) {
            this.x--;
        }
        
        this.calculateShadowY();
        this.draw();
    }
    
    // Rotate with 3D animation
    rotate(direction) {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Determine next pattern
        let nextPattern;
        if (direction === 'right') {
            nextPattern = (this.tetrominoN + 1) % this.tetromino.length;
        } else {
            nextPattern = (this.tetrominoN - 1 + this.tetromino.length) % this.tetromino.length;
        }
        
        // Check for wall kicks
        let kick = 0;
        if (this.collision(0, 0, this.tetromino[nextPattern])) {
            if (this.x > COLS / 2) {
                // Right wall collision
                kick = -1;
            } else {
                // Left wall collision
                kick = 1;
            }
        }
        
        // If animations are disabled, apply change immediately
        if (!enableSpinAnimations) {
            // Apply pattern if not colliding
            if (!this.collision(kick, 0, this.tetromino[nextPattern])) {
                this.x += kick;
                this.tetrominoN = nextPattern;
                this.activeTetromino = this.tetromino[this.tetrominoN];
                this.shadowTetromino = this.activeTetromino;
                
                // Update shadow position
                this.calculateShadowY();
                
                // Play rotation sound
                playPieceSound('rotate');
                
                // Draw the piece
                this.draw();
            }
            return;
        }
        
        // For animated version, start rotation transition
        this.rotationTransition = true;
        this.rotationDirection = direction === 'right' ? 'rotateRight' : 'rotateLeft';
        this.rotationProgress = 0;
        
        // Store current tetromino
        this.originalTetromino = this.activeTetromino;
        
        // Set target tetrominoN and position
        this.targetPattern = nextPattern;
        this.targetKick = kick;
        
        // Play rotation sound
        playPieceSound('rotate');
        
        // Start rotation animation
        this.animate3DRotation();
    }
    
    // Hard drop
    hardDrop() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Move down until a collision is detected
        while (!this.collision(0, 1, this.activeTetromino)) {
            this.y++;
        }
        
        // Lock the piece
        this.lock();
        
        // Get new piece
        p = randomPiece();
        
        // Calculate shadow for new piece
        p.calculateShadowY();
        
        // Draw the new piece
        p.draw();
        
        // Play hard drop sound
        playPieceSound('hardDrop');
    }
    
    // 3D horizontal rotation effect (around Y axis)
    rotate3DY() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Create a mirrored version that will be the result
        const rows = this.activeTetromino.length;
        const cols = this.activeTetromino[0].length;
        let mirroredTetromino = Array(rows).fill().map(() => Array(cols).fill(0));
        
        // Mirror horizontally (reverse each row)
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                mirroredTetromino[r][c] = this.activeTetromino[r][cols - 1 - c];
            }
        }
        
        // If animations are disabled, apply change immediately
        if (!enable3DEffects) {
            // Check if the mirrored piece would collide with anything
            if (!this.collision(0, 0, mirroredTetromino)) {
                // Apply the mirror
                this.activeTetromino = mirroredTetromino;
                this.shadowTetromino = mirroredTetromino;
                
                // Update shadow
                this.calculateShadowY();
                
                // Play sound
                playPieceSound('rotate');
                
                // Draw the piece
                this.draw();
            }
            return;
        }
        
        // Start rotation transition (animated version)
        this.rotationTransition = true;
        this.rotationDirection = 'horizontal';
        this.rotationProgress = 0;
        
        // Store current tetromino for when animation completes
        this.originalTetromino = this.activeTetromino;
        
        // Store target tetromino
        this.targetTetromino = mirroredTetromino;
        
        // Play rotation sound
        playPieceSound('rotate');
        
        // Start rotation animation
        this.animate3DRotation();
    }
    
    // 3D vertical rotation effect (around X axis)
    rotate3DX() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Create a mirrored version that will be the result
        const rows = this.activeTetromino.length;
        const cols = this.activeTetromino[0].length;
        let mirroredTetromino = Array(rows).fill().map(() => Array(cols).fill(0));
        
        // Mirror vertically (reverse each column)
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                mirroredTetromino[r][c] = this.activeTetromino[rows - 1 - r][c];
            }
        }
        
        // If animations are disabled, apply change immediately
        if (!enable3DEffects) {
            // Check if the mirrored piece would collide with anything
            if (!this.collision(0, 0, mirroredTetromino)) {
                // Apply the mirror
                this.activeTetromino = mirroredTetromino;
                this.shadowTetromino = mirroredTetromino;
                
                // Update shadow
                this.calculateShadowY();
                
                // Play sound
                playPieceSound('rotate');
                
                // Draw the piece
                this.draw();
            }
            return;
        }
        
        // Start rotation transition (animated version)
        this.rotationTransition = true;
        this.rotationDirection = 'vertical';
        this.rotationProgress = 0;
        
        // Store current tetromino for when animation completes
        this.originalTetromino = this.activeTetromino;
        
        // Store target tetromino
        this.targetTetromino = mirroredTetromino;
        
        // Play rotation sound
        playPieceSound('rotate');
        
        // Start rotation animation
        this.animate3DRotation();
    }
    
    // Collision detection
    collision(x = 0, y = 0, piece = this.activeTetromino) {
        for (let r = 0; r < piece.length; r++) {
            for (let c = 0; c < piece[r].length; c++) {
                if (!piece[r][c]) {
                    continue;
                }
                
                // Coordinates after movement
                let newX = this.x + c + (x || 0);
                let newY = this.y + r + (y || 0);
                
                // Conditions
                if (newX < 0 || newX >= COLS || newY >= ROWS) {
                    return true;
                }
                
                // Skip newY < 0; board[-1] will crash game
                if (newY < 0) {
                    continue;
                }
                
                // Check if there's a locked piece already
                if (board[newY][newX] !== EMPTY) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Lock the piece
    lock() {
        for (let r = 0; r < this.activeTetromino.length; r++) {
            for (let c = 0; c < this.activeTetromino[r].length; c++) {
                if (!this.activeTetromino[r][c]) {
                    continue;
                }
                
                // Game over if piece is above the board
                if (this.y + r < 0) {
                    gameOver = true;
                    break;
                }
                
                // Lock the piece
                board[this.y + r][this.x + c] = this.color;
            }
        }
        
        // Remove full rows and track their positions for fireworks
        let linesCleared = 0;
        let clearedRows = [];
        
        for (let r = 0; r < ROWS; r++) {
            let isRowFull = true;
            for (let c = 0; c < COLS; c++) {
                isRowFull = isRowFull && (board[r][c] !== EMPTY);
            }
            
            if (isRowFull) {
                // Store the row index for fireworks
                clearedRows.push(r);
                
                // Remove the row
                for (let y = r; y > 1; y--) {
                    for (let c = 0; c < COLS; c++) {
                        board[y][c] = board[y-1][c];
                    }
                }
                
                // Top row
                for (let c = 0; c < COLS; c++) {
                    board[0][c] = EMPTY;
                }
                
                linesCleared++;
            }
        }
        
        // Create fireworks for each cleared row
        if (clearedRows.length > 0) {
            for (let i = 0; i < clearedRows.length; i++) {
                // Create multiple fireworks along the row
                for (let j = 0; j < 3; j++) {
                    const x = (Math.random() * COLS * BLOCK_SIZE) + BLOCK_SIZE/2;
                    const y = (clearedRows[i] * BLOCK_SIZE) + BLOCK_SIZE/2;
                    fireworks.push(new Firework(x, y));
                }
            }
            
            // Play sound effect for line clear
            playLineClearSound(clearedRows.length);
        }
        
        // Update score
        if (linesCleared > 0) {
            // Points increase for multiple lines cleared at once
            const linePoints = [0, 100, 300, 500, 800]; // 0, 1, 2, 3, 4 lines
            score += linePoints[linesCleared] * level;
            lines += linesCleared;
            
            // Level up every 10 lines
            if (Math.floor(lines / 10) > level - 1) {
                level = Math.floor(lines / 10) + 1;
                // Speed up the game as level increases
                clearInterval(gameInterval);
                gameInterval = setInterval(dropPiece, Math.max(100, 1000 - (level * 100)));
            }
            
            // Update UI
            scoreElement.textContent = score;
            levelElement.textContent = level;
            linesElement.textContent = lines;
        }
        
        // Check for game over
        if (gameOver) {
            showGameOver();
        }
    }
    
    // Mirror horizontally (flip left-right)
    mirrorHorizontal() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Create a mirrored version of the current piece
        const rows = this.activeTetromino.length;
        const cols = this.activeTetromino[0].length;
        let mirroredTetromino = Array(rows).fill().map(() => Array(cols).fill(0));
        
        // Mirror horizontally (reverse each row)
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                mirroredTetromino[r][c] = this.activeTetromino[r][cols - 1 - c];
            }
        }
        
        // Check if the mirrored piece would collide with anything
        if (!this.collision(0, 0, mirroredTetromino)) {
            // Apply the mirror
            this.activeTetromino = mirroredTetromino;
            this.shadowTetromino = mirroredTetromino;
            
            // Update shadow
            this.calculateShadowY();
            
            // Play sound
            playPieceSound('rotate');
        }
        
        this.draw();
    }
    
    // Mirror vertically (flip top-bottom)
    mirrorVertical() {
        // Clear previous position
        this.undraw();
        clearPreviousPiecePosition();
        
        // Create a mirrored version of the current piece
        const rows = this.activeTetromino.length;
        const cols = this.activeTetromino[0].length;
        let mirroredTetromino = Array(rows).fill().map(() => Array(cols).fill(0));
        
        // Mirror vertically (reverse each column)
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                mirroredTetromino[r][c] = this.activeTetromino[rows - 1 - r][c];
            }
        }
        
        // Check if the mirrored piece would collide with anything
        if (!this.collision(0, 0, mirroredTetromino)) {
            // Apply the mirror
            this.activeTetromino = mirroredTetromino;
            this.shadowTetromino = mirroredTetromino;
            
            // Update shadow
            this.calculateShadowY();
            
            // Play sound
            playPieceSound('rotate');
        }
        
        this.draw();
    }
    
    // Animation function for 3D rotation
    animate3DRotation() {
        if (!this.rotationTransition && !this.showCompletionEffect) return;
        
        if (this.rotationTransition) {
            // Increment progress - use the global animation speed
            this.rotationProgress += animationSpeed;
            
            // When rotation is complete
            if (this.rotationProgress >= 1) {
                this.rotationTransition = false;
                this.showCompletionEffect = enable3DEffects; // Only show completion effect if 3D effects are enabled
                this.completionEffectProgress = 0;
                
                // Apply the target pattern/tetromino
                if (this.rotationDirection === 'horizontal' || this.rotationDirection === 'vertical') {
                    // Apply the mirror if it doesn't cause collision
                    if (!this.collision(0, 0, this.targetTetromino)) {
                        this.activeTetromino = this.targetTetromino;
                        this.shadowTetromino = this.activeTetromino;
                    }
                } else if (this.rotationDirection === 'rotateLeft' || this.rotationDirection === 'rotateRight') {
                    // Apply the rotation if it doesn't cause collision
                    if (!this.collision(this.targetKick, 0, this.tetromino[this.targetPattern])) {
                        this.x += this.targetKick;
                        this.tetrominoN = this.targetPattern;
                        this.activeTetromino = this.tetromino[this.tetrominoN];
                        this.shadowTetromino = this.activeTetromino;
                    }
                }
                
                // Update shadow position
                this.calculateShadowY();
                
                // If no completion effect, we're done
                if (!this.showCompletionEffect) {
                    this.draw();
                    return;
                }
            }
            
            // Calculate current rotation angles based on progress with easing
            let progress = this.rotationProgress;
            if (this.rotationEasing) {
                // Apply easing function (sinusoidal)
                progress = 0.5 - 0.5 * Math.cos(Math.PI * progress);
            }
            
            if (this.rotationDirection === 'horizontal') {
                this.rotationAngleY = Math.PI * progress;
            } else if (this.rotationDirection === 'vertical') {
                this.rotationAngleX = Math.PI * progress;
            } else if (this.rotationDirection === 'rotateRight') {
                // Rotate around Z axis (clockwise)
                this.rotationAngleZ = Math.PI * 2 * progress;
            } else if (this.rotationDirection === 'rotateLeft') {
                // Rotate around Z axis (counter-clockwise)
                this.rotationAngleZ = -Math.PI * 2 * progress;
            }
        } else if (this.showCompletionEffect) {
            // Handle completion effect animation
            this.completionEffectProgress += 0.1;
            
            if (this.completionEffectProgress >= 1) {
                this.showCompletionEffect = false;
                // Reset rotation angles
                this.rotationAngleX = 0;
                this.rotationAngleY = 0;
                this.rotationAngleZ = 0;
            }
        }
        
        // Redraw with current rotation
        this.draw();
        
        // Continue animation
        requestAnimationFrame(() => this.animate3DRotation());
    }
    
    // New method to draw with 3D rotation effect
    draw3D() {
        const tetromino = this.originalTetromino;
        
        const size = tetromino.length;
        const centerX = this.x + size / 2;
        const centerY = this.y + size / 2;
        
        // Sort blocks by depth for proper rendering
        let blocks = [];
        
        for (let r = 0; r < tetromino.length; r++) {
            for (let c = 0; c < tetromino[r].length; c++) {
                if (!tetromino[r][c]) continue;
                
                // Calculate position relative to center
                const relX = c - size / 2 + 0.5;
                const relY = r - size / 2 + 0.5;
                
                // Apply rotation transformation
                let transX = relX;
                let transY = relY;
                let scale = 1;
                let depth = 0;
                
                if (this.rotationDirection === 'horizontal') {
                    // Horizontal rotation (around Y axis)
                    const angle = this.rotationAngleY;
                    scale = Math.cos(angle);
                    
                    // For horizontal mirror, we flip across vertical axis (x changes)
                    transX = relX * Math.cos(angle);
                    depth = relX * Math.sin(angle);
                    
                    // Add perspective effect
                    const perspective = 0.2;
                    const perspectiveScale = 1 + depth * perspective;
                    transX /= perspectiveScale;
                    transY /= perspectiveScale;
                    scale /= perspectiveScale;
                } else if (this.rotationDirection === 'vertical') {
                    // Vertical rotation (around X axis)
                    const angle = this.rotationAngleX;
                    scale = Math.cos(angle);
                    
                    // For vertical mirror, we flip across horizontal axis (y changes)
                    transY = relY * Math.cos(angle);
                    depth = relY * Math.sin(angle);
                    
                    // Add perspective effect
                    const perspective = 0.2;
                    const perspectiveScale = 1 + depth * perspective;
                    transX /= perspectiveScale;
                    transY /= perspectiveScale;
                    scale /= perspectiveScale;
                } else if (this.rotationDirection === 'rotateRight' || this.rotationDirection === 'rotateLeft') {
                    // Z-axis rotation for regular tetris rotations
                    const angle = this.rotationAngleZ;
                    
                    // Apply rotation matrix
                    transX = relX * Math.cos(angle) - relY * Math.sin(angle);
                    transY = relX * Math.sin(angle) + relY * Math.cos(angle);
                    
                    // Keep full scale for Z rotation
                    scale = 1.0;
                    
                    // Add subtle depth effect based on rotation progress
                    const rotationProgress = Math.abs(angle) / (Math.PI * 2);
                    depth = 0.3 * Math.sin(rotationProgress * Math.PI);
                }
                
                // Apply transformation
                const newX = centerX + transX - 0.5;
                const newY = centerY + transY - 0.5;
                
                // Store block info for depth sorting
                blocks.push({
                    x: newX,
                    y: newY,
                    color: this.color,
                    scale: scale,
                    depth: depth
                });
            }
        }
        
        // Sort blocks by depth (back to front)
        blocks.sort((a, b) => a.depth - b.depth);
        
        // Draw blocks in sorted order
        for (const block of blocks) {
            if (block.scale > 0) {
                // Apply scale to create 3D effect with depth
                draw3DSquare(block.x, block.y, this.color, block.scale, block.depth);
            }
        }
    }
    
    // Draw completion effect
    drawCompletionEffect() {
        // Draw shadow if enabled
        if (showShadow) {
            this.drawShadow();
        }
        
        // Draw with glow/highlight effect
        for (let r = 0; r < this.activeTetromino.length; r++) {
            for (let c = 0; c < this.activeTetromino[r].length; c++) {
                if (!this.activeTetromino[r][c]) continue;
                
                // Calculate effect scale/pulse based on progress
                const pulseFactor = 1 + 0.2 * Math.sin(this.completionEffectProgress * Math.PI * 2);
                
                // Draw with highlight effect
                drawHighlightSquare(this.x + c, this.y + r, this.color, this.completionEffectProgress, pulseFactor);
            }
        }
    }
}

// Draw a shadow square on the board
function drawShadowSquare(x, y) {
    if (y < 0) return; // Don't draw above the board
    
    // Draw a semi-transparent outline for the shadow
    ctx.globalAlpha = 0.3;
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.strokeRect(x * BLOCK_SIZE + 2, y * BLOCK_SIZE + 2, BLOCK_SIZE - 4, BLOCK_SIZE - 4);
    
    // Add a fill for better visibility
    ctx.fillStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.fillRect(x * BLOCK_SIZE + 4, y * BLOCK_SIZE + 4, BLOCK_SIZE - 8, BLOCK_SIZE - 8);
    
    ctx.globalAlpha = 1.0; // Reset alpha
}

// Draw a square on the board with gradient and glow
function drawSquare(x, y, color) {
    if (y < 0) return; // Don't draw above the board
    
    if (color === EMPTY) {
        // Draw empty square
        ctx.fillStyle = color;
        ctx.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        ctx.strokeStyle = '#333';
        ctx.strokeRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
    } else {
        // Save the context state
        ctx.save();
        
        // Create gradient for filled squares
        const gradient = ctx.createLinearGradient(
            x * BLOCK_SIZE, 
            y * BLOCK_SIZE, 
            (x + 1) * BLOCK_SIZE, 
            (y + 1) * BLOCK_SIZE
        );
        
        const gradColors = GRADIENT_COLORS[color] || [color, color];
        gradient.addColorStop(0, gradColors[0]);
        gradient.addColorStop(1, gradColors[1]);
        
        // Add glow effect
        ctx.shadowColor = gradColors[0];
        ctx.shadowBlur = 10;
        
        // Fill with gradient
        ctx.fillStyle = gradient;
        ctx.fillRect(x * BLOCK_SIZE, y * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        
        // Reset shadow for clean edges
        ctx.shadowColor = 'transparent';
        ctx.shadowBlur = 0;
        
        // Add highlights and shadow
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(x * BLOCK_SIZE, y * BLOCK_SIZE);
        ctx.lineTo((x + 1) * BLOCK_SIZE, y * BLOCK_SIZE);
        ctx.lineTo((x + 1) * BLOCK_SIZE, (y + 0.3) * BLOCK_SIZE);
        ctx.moveTo(x * BLOCK_SIZE, y * BLOCK_SIZE);
        ctx.lineTo(x * BLOCK_SIZE, (y + 1) * BLOCK_SIZE);
        ctx.lineTo((x + 0.3) * BLOCK_SIZE, (y + 1) * BLOCK_SIZE);
        ctx.stroke();
        
        ctx.strokeStyle = '#333';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo((x + 1) * BLOCK_SIZE, y * BLOCK_SIZE);
        ctx.lineTo((x + 1) * BLOCK_SIZE, (y + 1) * BLOCK_SIZE);
        ctx.lineTo(x * BLOCK_SIZE, (y + 1) * BLOCK_SIZE);
        ctx.stroke();
        
        // Add inner glow
        ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
        const innerPadding = 4;
        ctx.fillRect(
            x * BLOCK_SIZE + innerPadding, 
            y * BLOCK_SIZE + innerPadding, 
            BLOCK_SIZE - innerPadding * 2, 
            BLOCK_SIZE - innerPadding * 2
        );
        
        // Restore the context state
        ctx.restore();
    }
}

// Draw a 3D square with rotation effect
function draw3DSquare(x, y, color, scale, depth = 0) {
    if (y < 0) return; // Don't draw above the board
    
    // Save the context state
    ctx.save();
    
    // Calculate the position with scaling from center of block
    const centerX = (x + 0.5) * BLOCK_SIZE;
    const centerY = (y + 0.5) * BLOCK_SIZE;
    const scaledSize = BLOCK_SIZE * Math.abs(scale);
    const offsetX = centerX - scaledSize / 2;
    const offsetY = centerY - scaledSize / 2;
    
    // Create gradient for filled squares
    const gradient = ctx.createLinearGradient(
        offsetX, 
        offsetY, 
        offsetX + scaledSize, 
        offsetY + scaledSize
    );
    
    const gradColors = GRADIENT_COLORS[color] || [color, color];
    
    // Adjust color based on depth
    const depthFactor = 0.7 + 0.3 * (1 - Math.min(1, Math.abs(depth)));
    const adjustColor = (color) => {
        // Simple color brightening/darkening based on depth
        if (color.startsWith('#')) {
            // Convert hex to RGB
            const r = parseInt(color.slice(1, 3), 16);
            const g = parseInt(color.slice(3, 5), 16);
            const b = parseInt(color.slice(5, 7), 16);
            
            // Adjust brightness
            const newR = Math.min(255, Math.floor(r * depthFactor));
            const newG = Math.min(255, Math.floor(g * depthFactor));
            const newB = Math.min(255, Math.floor(b * depthFactor));
            
            // Convert back to hex
            return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
        }
        return color;
    };
    
    const adjustedColors = [
        adjustColor(gradColors[0]),
        adjustColor(gradColors[1])
    ];
    
    gradient.addColorStop(0, adjustedColors[0]);
    gradient.addColorStop(1, adjustedColors[1]);
    
    // Add glow effect
    ctx.shadowColor = adjustedColors[0];
    ctx.shadowBlur = 10 * Math.abs(scale);
    
    // Fill with gradient
    ctx.fillStyle = gradient;
    ctx.fillRect(offsetX, offsetY, scaledSize, scaledSize);
    
    // Reset shadow for clean edges
    ctx.shadowColor = 'transparent';
    ctx.shadowBlur = 0;
    
    // Scale line width with the block
    const lineWidth = 2 * Math.abs(scale);
    
    // Add highlights and shadow - scaled
    ctx.strokeStyle = '#fff';
    ctx.globalAlpha = depthFactor; // Make lines fade with depth
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.moveTo(offsetX, offsetY);
    ctx.lineTo(offsetX + scaledSize, offsetY);
    ctx.lineTo(offsetX + scaledSize, offsetY + scaledSize * 0.3);
    ctx.moveTo(offsetX, offsetY);
    ctx.lineTo(offsetX, offsetY + scaledSize);
    ctx.lineTo(offsetX + scaledSize * 0.3, offsetY + scaledSize);
    ctx.stroke();
    
    ctx.strokeStyle = '#333';
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    ctx.moveTo(offsetX + scaledSize, offsetY);
    ctx.lineTo(offsetX + scaledSize, offsetY + scaledSize);
    ctx.lineTo(offsetX, offsetY + scaledSize);
    ctx.stroke();
    
    // Add inner glow
    ctx.fillStyle = `rgba(255, 255, 255, ${0.2 * depthFactor})`;
    const innerPadding = 4 * Math.abs(scale);
    ctx.fillRect(
        offsetX + innerPadding, 
        offsetY + innerPadding, 
        scaledSize - innerPadding * 2, 
        scaledSize - innerPadding * 2
    );
    
    // Restore the context state
    ctx.restore();
}

// Draw a square with highlight completion effect
function drawHighlightSquare(x, y, color, progress, pulseFactor = 1) {
    if (y < 0) return; // Don't draw above the board
    
    // Save the context state
    ctx.save();
    
    // Calculate the position
    const offsetX = x * BLOCK_SIZE;
    const offsetY = y * BLOCK_SIZE;
    
    // Create gradient with highlight
    const gradient = ctx.createLinearGradient(
        offsetX, 
        offsetY, 
        offsetX + BLOCK_SIZE, 
        offsetY + BLOCK_SIZE
    );
    
    const gradColors = GRADIENT_COLORS[color] || [color, color];
    
    // Brighten colors for highlight effect
    const highlightFactor = 1.3 - 0.3 * progress; // Fades over time
    const brightColor1 = highlightColor(gradColors[0], highlightFactor);
    const brightColor2 = highlightColor(gradColors[1], highlightFactor);
    
    gradient.addColorStop(0, brightColor1);
    gradient.addColorStop(1, brightColor2);
    
    // Add stronger glow effect
    ctx.shadowColor = brightColor1;
    ctx.shadowBlur = 15 * (1.5 - progress); // Fades over time
    
    // Draw slightly larger block during pulse
    const pulseOffset = (pulseFactor - 1) * BLOCK_SIZE / 2;
    const pulseSize = BLOCK_SIZE * pulseFactor;
    
    // Fill with gradient
    ctx.fillStyle = gradient;
    ctx.fillRect(
        offsetX - pulseOffset,
        offsetY - pulseOffset, 
        pulseSize, 
        pulseSize
    );
    
    // Add highlights and shadow
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2 * pulseFactor;
    ctx.beginPath();
    ctx.moveTo(offsetX - pulseOffset, offsetY - pulseOffset);
    ctx.lineTo(offsetX + pulseSize - pulseOffset, offsetY - pulseOffset);
    ctx.lineTo(offsetX + pulseSize - pulseOffset, offsetY + pulseSize * 0.3 - pulseOffset);
    ctx.moveTo(offsetX - pulseOffset, offsetY - pulseOffset);
    ctx.lineTo(offsetX - pulseOffset, offsetY + pulseSize - pulseOffset);
    ctx.lineTo(offsetX + pulseSize * 0.3 - pulseOffset, offsetY + pulseSize - pulseOffset);
    ctx.stroke();
    
    // Restore the context state
    ctx.restore();
}

// Helper function to brighten a color
function highlightColor(color, factor) {
    if (color.startsWith('#')) {
        // Convert hex to RGB
        const r = parseInt(color.slice(1, 3), 16);
        const g = parseInt(color.slice(3, 5), 16);
        const b = parseInt(color.slice(5, 7), 16);
        
        // Brighten
        const newR = Math.min(255, Math.floor(r * factor));
        const newG = Math.min(255, Math.floor(g * factor));
        const newB = Math.min(255, Math.floor(b * factor));
        
        // Convert back to hex
        return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`;
    }
    return color;
}

// Draw a square in the preview area with glow
function drawPreviewSquare(x, y, color) {
    // Save the context state
    nextPieceCtx.save();
    
    // Create gradient for filled squares
    const gradient = nextPieceCtx.createLinearGradient(
        x * PREVIEW_BLOCK_SIZE, 
        y * PREVIEW_BLOCK_SIZE, 
        (x + 1) * PREVIEW_BLOCK_SIZE, 
        (y + 1) * PREVIEW_BLOCK_SIZE
    );
    
    const gradColors = GRADIENT_COLORS[color] || [color, color];
    gradient.addColorStop(0, gradColors[0]);
    gradient.addColorStop(1, gradColors[1]);
    
    // Add glow
    nextPieceCtx.shadowColor = gradColors[0];
    nextPieceCtx.shadowBlur = 8;
    
    // Fill with gradient
    nextPieceCtx.fillStyle = gradient;
    nextPieceCtx.fillRect(x * PREVIEW_BLOCK_SIZE, y * PREVIEW_BLOCK_SIZE, PREVIEW_BLOCK_SIZE, PREVIEW_BLOCK_SIZE);
    
    // Reset shadow for clean edges
    nextPieceCtx.shadowColor = 'transparent';
    nextPieceCtx.shadowBlur = 0;
    
    // Add highlights and shadow
    nextPieceCtx.strokeStyle = '#fff';
    nextPieceCtx.lineWidth = 1;
    nextPieceCtx.beginPath();
    nextPieceCtx.moveTo(x * PREVIEW_BLOCK_SIZE, y * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo((x + 1) * PREVIEW_BLOCK_SIZE, y * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo((x + 1) * PREVIEW_BLOCK_SIZE, (y + 0.3) * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.moveTo(x * PREVIEW_BLOCK_SIZE, y * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo(x * PREVIEW_BLOCK_SIZE, (y + 1) * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo((x + 0.3) * PREVIEW_BLOCK_SIZE, (y + 1) * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.stroke();
    
    nextPieceCtx.strokeStyle = '#333';
    nextPieceCtx.lineWidth = 1;
    nextPieceCtx.beginPath();
    nextPieceCtx.moveTo((x + 1) * PREVIEW_BLOCK_SIZE, y * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo((x + 1) * PREVIEW_BLOCK_SIZE, (y + 1) * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.lineTo(x * PREVIEW_BLOCK_SIZE, (y + 1) * PREVIEW_BLOCK_SIZE);
    nextPieceCtx.stroke();
    
    // Add inner glow
    nextPieceCtx.fillStyle = 'rgba(255, 255, 255, 0.2)';
    const innerPadding = 2;
    nextPieceCtx.fillRect(
        x * PREVIEW_BLOCK_SIZE + innerPadding, 
        y * PREVIEW_BLOCK_SIZE + innerPadding, 
        PREVIEW_BLOCK_SIZE - innerPadding * 2, 
        PREVIEW_BLOCK_SIZE - innerPadding * 2
    );
    
    // Restore the context state
    nextPieceCtx.restore();
}

// Draw the board
function drawBoard() {
    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            drawSquare(c, r, board[r][c]);
        }
    }
}

// Generate random piece
function randomPiece() {
    // Initialize bags if they're empty
    if (pieceBag.length === 0 && nextBag.length === 0) {
        pieceBag = generateBag();
        nextBag = generateBag();
    }
    
    return getNextPieceFromBag();
}

// Play piece movement sounds
function playPieceSound(type) {
    let sound;
    
    switch(type) {
        case 'move':
            sound = moveSound;
            break;
        case 'rotate':
            sound = rotateSound;
            break;
        case 'drop':
            sound = dropSound;
            break;
    }
    
    if (sound) {
        sound.currentTime = 0;
        sound.volume = 0.2;
        sound.play().catch(err => console.log('Audio play error:', err));
    }
}

// Play sound effects for line clear
function playLineClearSound(lineCount) {
    // Different sounds based on how many lines were cleared
    const sound = new Audio();
    
    if (lineCount === 4) {
        // Tetris!
        sound.src = 'https://assets.mixkit.co/sfx/preview/mixkit-achievement-bell-600.mp3';
    } else if (lineCount >= 2) {
        // Multiple lines
        sound.src = 'https://assets.mixkit.co/sfx/preview/mixkit-arcade-game-complete-or-approved-mission-205.mp3';
    } else {
        // Single line
        sound.src = 'https://assets.mixkit.co/sfx/preview/mixkit-unlock-game-notification-253.mp3';
    }
    
    sound.volume = 0.3;
    sound.play().catch(err => console.log('Audio play error:', err));
}

// Update game frame
function update() {
    // Update and draw fireworks
    for (let i = fireworks.length - 1; i >= 0; i--) {
        fireworks[i].update();
        
        if (fireworks[i].isDone) {
            fireworks.splice(i, 1);
        }
    }
    
    // Request the next frame
    requestAnimationFrame(update);
}

// Draw game elements
function draw() {
    // Clear the entire canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Draw a border around the play area
    ctx.strokeStyle = 'rgba(80, 80, 80, 0.6)';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);
    
    // Clear any previous piece positions
    clearPreviousPiecePosition();
    
    // Draw the board - this shows only locked pieces
    drawBoard();
    
    // Always draw the active piece
    if (p) {
        p.draw();
    }
    
    // Draw fireworks (clipped to canvas area)
    ctx.save();
    ctx.beginPath();
    ctx.rect(0, 0, canvas.width, canvas.height);
    ctx.clip();
    for (let i = 0; i < fireworks.length; i++) {
        fireworks[i].draw();
    }
    ctx.restore();
    
    // Draw messages if game is paused
    if (paused) {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = 'white';
        ctx.font = '30px "Press Start 2P"';
        ctx.textAlign = 'center';
        ctx.fillText('PAUSED', canvas.width / 2, canvas.height / 2);
    }
    
    // Request the next frame
    requestAnimationFrame(draw);
}

// Drop the piece - called by interval
function dropPiece() {
    if (gameOver || paused) return;
    
    let now = Date.now();
    let delta = now - dropStart;
    
    // Drop speed depends on level
    let speed = 1000 * (1 - (level - 1) * 0.1);
    speed = Math.max(speed, 100); // Don't allow too fast drops (minimum 100ms)
    
    if (delta > speed) {
        p.moveDown();
        dropStart = now;
        
        // If we can't move down and we're still at the top, game over
        if (p.collision(0, 1) && p.y < 1) {
            gameOver = true;
            showGameOver();
        }
    }
    
    if (!gameOver) requestAnimationFrame(draw);
}

// Show game over modal
function showGameOver() {
    clearInterval(gameInterval);
    finalScoreElement.textContent = score;
    gameOverModal.classList.add('active');
}

// Reset the game
function resetGame() {
    // Reset game variables
    score = 0;
    level = 1;
    lines = 0;
    gameOver = false;
    paused = false;
    
    // Clear the board
    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            board[r][c] = EMPTY;
        }
    }
    
    // Update UI
    scoreElement.textContent = score;
    levelElement.textContent = level;
    linesElement.textContent = lines;
    
    // Close game over modal if open
    gameOverModal.classList.remove('active');
    
    // Generate pieces
    p = randomPiece();
    nextPiece = randomPiece();
    nextPiece.drawNextPiece();
    
    // Start the game interval
    dropStart = Date.now();
    clearInterval(gameInterval);
    gameInterval = setInterval(dropPiece, 1000);
}

// Toggle pause
function togglePause() {
    paused = !paused;
    
    if (paused) {
        clearInterval(gameInterval);
        pauseBtn.textContent = "Resume";
    } else {
        gameInterval = setInterval(dropPiece, Math.max(100, 1000 - (level * 100)));
        pauseBtn.textContent = "Pause";
    }
}

// Toggle options modal
function toggleOptionsModal() {
    if (optionsModal.classList.contains('active')) {
        optionsModal.classList.remove('active');
    } else {
        optionsModal.classList.add('active');
    }
}

// Apply options settings
function applyOptions() {
    enable3DEffects = toggle3DEffects.checked;
    enableSpinAnimations = toggleSpinAnimations.checked;
    animationSpeed = parseFloat(animationSpeedSlider.value);
    forceMobileControls = toggleMobileControls.checked;
    
    // Apply mobile controls if checked or if on mobile device
    if (forceMobileControls) {
        if (!touchControls) {
            initTouchControls();
            touchControls = true;
        }
        document.body.classList.add('mobile-mode');
    } else if (!isMobile) {
        document.body.classList.remove('mobile-mode');
    }
    
    // Save options
    saveOptions();
}

// Save options to localStorage
function saveOptions() {
    localStorage.setItem('tetris3DOptions', JSON.stringify({
        enable3DEffects,
        enableSpinAnimations,
        animationSpeed,
        forceMobileControls
    }));
}

// Load options from localStorage
function loadOptions() {
    const savedOptions = localStorage.getItem('tetris3DOptions');
    if (savedOptions) {
        const options = JSON.parse(savedOptions);
        
        enable3DEffects = options.enable3DEffects !== undefined ? options.enable3DEffects : true;
        enableSpinAnimations = options.enableSpinAnimations !== undefined ? options.enableSpinAnimations : true;
        animationSpeed = options.animationSpeed !== undefined ? options.animationSpeed : 0.05;
        forceMobileControls = options.forceMobileControls !== undefined ? options.forceMobileControls : false;
        
        // Update UI controls
        toggle3DEffects.checked = enable3DEffects;
        toggleSpinAnimations.checked = enableSpinAnimations;
        animationSpeedSlider.value = animationSpeed;
        
        if (toggleMobileControls) {
            toggleMobileControls.checked = forceMobileControls;
        }
    }
}

// Initialize game
function init() {
    // Reset canvas to ensure clean state
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    
    // Load saved options
    loadOptions();
    
    // Draw the board
    drawBoard();
    
    // Generate initial pieces
    p = randomPiece();
    nextPiece = randomPiece();
    
    // Draw initial next piece
    nextPiece.drawNextPiece();
    
    // Set up game interval
    dropStart = Date.now();
    gameInterval = setInterval(dropPiece, 1000);
    
    // Listen for keyboard events
    document.addEventListener('keydown', control);
    
    // Add resize listener for responsive layout
    window.addEventListener('resize', handleResize);
    
    // Initial resize to set correct dimensions
    handleResize();
    
    // Button event listeners
    startBtn.addEventListener('click', resetGame);
    pauseBtn.addEventListener('click', togglePause);
    playAgainBtn.addEventListener('click', resetGame);
    shadowBtn.addEventListener('click', toggleShadow);
    optionsBtn.addEventListener('click', toggleOptionsModal);
    optionsCloseBtn.addEventListener('click', toggleOptionsModal);
    
    // Options event listeners
    toggle3DEffects.addEventListener('change', function() {
        applyOptions();
    });
    
    toggleSpinAnimations.addEventListener('change', function() {
        applyOptions();
    });
    
    animationSpeedSlider.addEventListener('input', function() {
        applyOptions();
    });
    
    if (toggleMobileControls) {
        toggleMobileControls.addEventListener('change', function() {
            applyOptions();
            
            // Force resize to update layout
            window.dispatchEvent(new Event('resize'));
        });
    }
    
    // Apply mobile mode if forced or on mobile device
    if (forceMobileControls || isMobile) {
        if (!touchControls) {
            initTouchControls();
            touchControls = true;
        }
        document.body.classList.add('mobile-mode');
    }
    
    // Initialize controller support
    initControllerSupport();
    
    // Initial controller status update
    updateControllerStatus();
}

// Handle window resize for responsive layout
function handleResize() {
    const gameContainer = document.querySelector('.game-container');
    const gameWrapper = document.querySelector('.game-wrapper');
    const scoreContainer = document.querySelector('.score-container');
    
    if (isMobile || forceMobileControls) {
        // Scale the canvas to fit mobile screen
        const viewportWidth = Math.min(window.innerWidth, document.documentElement.clientWidth);
        const viewportHeight = Math.min(window.innerHeight, document.documentElement.clientHeight);
        
        // Detect orientation
        const isPortrait = viewportHeight > viewportWidth;
        
        // Calculate available game area (accounting for UI elements)
        const titleHeight = 40; // Estimate for title
        const scoreWidth = isPortrait ? 120 : 100; // Width for score container in portrait/landscape
        const availableWidth = viewportWidth - scoreWidth - 20; // Subtract score width + padding
        const availableHeight = viewportHeight - titleHeight - 20; // Subtract title height + padding
        
        // Calculate optimal dimensions while maintaining aspect ratio
        const gameRatio = ROWS / COLS;
        
        // Calculate scale based on available space
        let targetWidth, targetHeight;
        
        if (isPortrait) {
            // For portrait, prioritize fitting the width
            targetWidth = availableWidth * 0.95;
            targetHeight = targetWidth * gameRatio;
            
            // If too tall, scale down based on height
            if (targetHeight > availableHeight * 0.95) {
                targetHeight = availableHeight * 0.95;
                targetWidth = targetHeight / gameRatio;
            }
        } else {
            // For landscape, prioritize fitting the height
            targetHeight = availableHeight * 0.95;
            targetWidth = targetHeight / gameRatio;
            
            // If too wide, scale down based on width
            if (targetWidth > availableWidth * 0.95) {
                targetWidth = availableWidth * 0.95;
                targetHeight = targetWidth * gameRatio;
            }
        }
        
        // Apply dimensions
        canvas.style.width = `${targetWidth}px`;
        canvas.style.height = `${targetHeight}px`;
        
        // Force a redraw of the nextPiece preview to fix rendering issues
        if (nextPiece) {
            setTimeout(() => {
                nextPieceCtx.clearRect(0, 0, nextPieceCanvas.width, nextPieceCanvas.height);
                nextPiece.drawNextPiece();
            }, 100);
        }
        
        // Show touch controls mode
        document.body.classList.add('mobile-mode');
    } else {
        // Reset to desktop layout
        canvas.style.width = '';
        canvas.style.height = '';
        
        document.body.classList.remove('mobile-mode');
    }
}

// Toggle shadow display
function toggleShadow() {
    showShadow = !showShadow;
    if (!gameOver && !paused) {
        // Redraw current piece to show/hide shadow
        p.undraw();
        p.draw();
    }
}

// Controller support
function initControllerSupport() {
    // Check for existing gamepads
    scanGamepads();
    
    // Listen for controller connection/disconnection
    window.addEventListener('gamepadconnected', (e) => {
        console.log('Gamepad connected:', e.gamepad.id);
        gamepadConnected = true;
        controllers[e.gamepad.index] = e.gamepad;
        
        // Start polling for controller input if not already
        if (!controllerInterval) {
            controllerInterval = setInterval(pollControllers, controllerPollingRate);
        }
        
        // Show controller connected message
        showControllerMessage(`Controller connected: ${e.gamepad.id}`);
    });
    
    window.addEventListener('gamepaddisconnected', (e) => {
        console.log('Gamepad disconnected:', e.gamepad.id);
        delete controllers[e.gamepad.index];
        
        // Check if there are any controllers left
        if (Object.keys(controllers).length === 0) {
            gamepadConnected = false;
            clearInterval(controllerInterval);
            controllerInterval = null;
        }
        
        // Show controller disconnected message
        showControllerMessage('Controller disconnected');
    });
    
    // Initial scan
    if (Object.keys(controllers).length > 0) {
        controllerInterval = setInterval(pollControllers, controllerPollingRate);
    }
}

// Scan for connected gamepads
function scanGamepads() {
    const gamepads = navigator.getGamepads ? navigator.getGamepads() : 
                    (navigator.webkitGetGamepads ? navigator.webkitGetGamepads() : []);
    
    for (let i = 0; i < gamepads.length; i++) {
        if (gamepads[i]) {
            controllers[gamepads[i].index] = gamepads[i];
            gamepadConnected = true;
        }
    }
}

// Poll controllers for input
function pollControllers() {
    if (!gamepadConnected || gameOver) return;
    
    // Get fresh gamepad data
    scanGamepads();
    
    // Use the first available controller
    const controller = controllers[Object.keys(controllers)[0]];
    if (!controller) return;
    
    // Initialize state object if needed
    if (!lastControllerState[controller.index]) {
        lastControllerState[controller.index] = {
            buttons: Array(controller.buttons.length).fill(false),
            axes: Array(controller.axes.length).fill(0)
        };
    }
    
    // Check buttons
    for (let action in controllerMapping) {
        const buttonIndex = controllerMapping[action][0];
        
        // Handle button presses
        if (buttonIndex >= 0 && buttonIndex < controller.buttons.length) {
            const button = controller.buttons[buttonIndex];
            const pressed = button.pressed || button.value > 0.5;
            
            // Check if this is a new press (wasn't pressed last time)
            if (pressed && !lastControllerState[controller.index].buttons[buttonIndex]) {
                // Handle controller action
                handleControllerAction(action);
            }
            
            // Update state
            lastControllerState[controller.index].buttons[buttonIndex] = pressed;
        }
    }
    
    // Handle analog stick for movement
    // Left stick horizontal
    if (controller.axes[0] < -0.5 && lastControllerState[controller.index].axes[0] >= -0.5) {
        handleControllerAction('left');
    } 
    else if (controller.axes[0] > 0.5 && lastControllerState[controller.index].axes[0] <= 0.5) {
        handleControllerAction('right');
    }
    
    // Left stick vertical
    if (controller.axes[1] > 0.5 && lastControllerState[controller.index].axes[1] <= 0.5) {
        handleControllerAction('down');
    }
    
    // Update axes state
    lastControllerState[controller.index].axes = [...controller.axes];
}

// Handle controller actions
function handleControllerAction(action) {
    if (paused && action !== 'pause') return;
    
    switch(action) {
        case 'left':
            p.moveLeft();
            break;
        case 'right':
            p.moveRight();
            break;
        case 'down':
            p.moveDown();
            break;
        case 'rotateLeft':
            p.rotate('left');
            break;
        case 'rotateRight':
            p.rotate('right');
            break;
        case 'mirrorH':
            p.mirrorHorizontal();
            break;
        case 'mirrorV':
            p.mirrorVertical();
            break;
        case 'hardDrop':
            p.hardDrop();
            break;
        case 'pause':
            togglePause();
            break;
    }
}

// Show controller message
function showControllerMessage(message) {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'controller-message';
    messageDiv.textContent = message;
    document.body.appendChild(messageDiv);
    
    // Remove after a delay
    setTimeout(() => {
        messageDiv.classList.add('fade-out');
        setTimeout(() => {
            document.body.removeChild(messageDiv);
        }, 500);
    }, 3000);
    
    // Update controller status indicator
    updateControllerStatus();
}

// Update controller status indicator
function updateControllerStatus() {
    if (gamepadConnected && Object.keys(controllers).length > 0) {
        const controller = controllers[Object.keys(controllers)[0]];
        controllerStatus.textContent = `Controller Connected: ${controller.id.slice(0, 20)}...`;
        controllerStatus.classList.remove('disconnected');
        controllerStatus.classList.add('connected');
    } else {
        controllerStatus.textContent = 'No Controller Detected';
        controllerStatus.classList.remove('connected');
        controllerStatus.classList.add('disconnected');
    }
}

// The control function
function control(event) {
    if (gameOver) return;
    if (paused && event.keyCode !== 80) return; // Allow only P key if paused
    
    switch(event.keyCode) {
        case 37: // Left arrow
        case 65: // A key
            p.moveLeft();
            break;
        case 38: // Up arrow - no function
            break;
        case 39: // Right arrow
        case 68: // D key
            p.moveRight();
            break;
        case 40: // Down arrow
        case 83: // S key
            p.moveDown();
            break;
        case 81: // Q key - rotate left
            p.rotate('left');
            break;
        case 69: // E key - rotate right
            p.rotate('right');
            break;
        case 87: // W key - 3D vertical rotation
            p.rotate3DX();
            break;
        case 88: // X key - 3D horizontal rotation
            p.rotate3DY();
            break;
        case 32: // Space bar - hard drop
            p.hardDrop();
            break;
        case 80: // P key - pause
            togglePause();
            break;
        case 72: // H key - toggle shadow
            toggleShadow();
            break;
    }
}

// Maintain a reference to the active piece's previous position for proper clearing
let previousPiecePosition = {
    x: 0,
    y: 0,
    shape: null,
    exists: false
};

// Always clear previous piece position before drawing new position
function clearPreviousPiecePosition() {
    if (previousPiecePosition.exists && previousPiecePosition.shape) {
        // Clear entire previous area with padding
        const padding = 5;
        const minX = Math.max(0, previousPiecePosition.x - padding);
        const maxX = Math.min(COLS - 1, previousPiecePosition.x + previousPiecePosition.shape[0].length + padding);
        const minY = Math.max(0, previousPiecePosition.y - padding);
        const maxY = Math.min(ROWS - 1, previousPiecePosition.y + previousPiecePosition.shape.length + padding);
        
        for (let r = minY; r <= maxY; r++) {
            for (let c = minX; c <= maxX; c++) {
                if (r >= 0 && c >= 0 && r < ROWS && c < COLS && board[r][c] === EMPTY) {
                    drawSquare(c, r, EMPTY);
                }
            }
        }
    }
}

// Touch control variables
let touchStartX = 0;
let touchStartY = 0;
let touchStartTime = 0;
const SWIPE_THRESHOLD = 15; // Reduced threshold for more responsive movement
const TAP_THRESHOLD = 200; // milliseconds
const DOUBLE_TAP_THRESHOLD = 400; // Increased to prevent accidental double-taps
const TRIPLE_TAP_THRESHOLD = 600; // Maximum time between first and third tap
let lastTapTime = 0;
let secondLastTapTime = 0; // Track time of second-to-last tap for triple tap detection
let lastMoveTime = 0;
let touchIdentifier = null;
let lastTapX = 0;
let lastTapY = 0;
let secondLastTapX = 0; // Track position of second-to-last tap
let secondLastTapY = 0; // Track position of second-to-last tap
const TAP_DISTANCE_THRESHOLD = 20; // Maximum distance to consider as same-position tap
const MOVE_COOLDOWN = 60; // Cooldown between moves to prevent too rapid movement

// Initialize touch controls
function initTouchControls() {
    // Add touch event listeners to the canvas
    canvas.addEventListener('touchstart', handleTouchStart, false);
    canvas.addEventListener('touchmove', handleTouchMove, { passive: false });
    canvas.addEventListener('touchend', handleTouchEnd, false);
    
    // Create on-screen control buttons
    createTouchControlButtons();
}

// Handle touch start event
function handleTouchStart(event) {
    if (gameOver || paused) return;
    event.preventDefault();
    
    // Only track single touches for gesture detection
    if (event.touches.length > 1) return;
    
    // Store the initial touch position
    const touch = event.touches[0];
    touchStartX = touch.clientX;
    touchStartY = touch.clientY;
    touchStartTime = Date.now();
    touchIdentifier = touch.identifier;
}

// Handle touch move event
function handleTouchMove(event) {
    if (gameOver || paused) return;
    event.preventDefault();
    
    // Skip if it's a multi-touch gesture
    if (event.touches.length > 1) return;
    
    const now = Date.now();
    
    if (!event.touches.length) return;
    
    const touch = event.touches[0];
    // Make sure we're tracking the same touch
    if (touch.identifier !== touchIdentifier) return;
    
    const diffX = touch.clientX - touchStartX;
    const diffY = touch.clientY - touchStartY;
    
    // Only process if movement is significant (prevent accidental moves)
    const absX = Math.abs(diffX);
    const absY = Math.abs(diffY);
    
    if (absX > SWIPE_THRESHOLD) {
        // Horizontal movement has priority and a shorter cooldown
        if (now - lastMoveTime >= MOVE_COOLDOWN) {
            if (diffX > 0) {
                p.moveRight();
            } else {
                p.moveLeft();
            }
            
            // Reset touch start to allow for continuous movement
            touchStartX = touch.clientX;
            lastMoveTime = now;
        }
    } 
    // Only handle downward swipes for soft drop - if no horizontal movement is detected
    else if (absY > SWIPE_THRESHOLD * 2 && absY > absX && diffY > 0) {
        if (now - lastMoveTime >= MOVE_COOLDOWN * 2) { // Use longer cooldown for down movement
            p.moveDown();
            touchStartY = touch.clientY;
            lastMoveTime = now;
        }
    }
}

// Handle touch end event
function handleTouchEnd(event) {
    if (gameOver || paused) return;
    event.preventDefault();
    
    const touchEndTime = Date.now();
    const touchDuration = touchEndTime - touchStartTime;
    
    // Get touch coordinates
    const touch = event.changedTouches[0];
    const touchX = touch.clientX;
    const touchY = touch.clientY;
    
    // Check for tap (quick touch)
    if (touchDuration < TAP_THRESHOLD) {
        // Calculate distances between taps
        const distanceFromLastTap = Math.sqrt(
            Math.pow(touchX - lastTapX, 2) + 
            Math.pow(touchY - lastTapY, 2)
        );
        
        const distanceFromSecondLastTap = Math.sqrt(
            Math.pow(touchX - secondLastTapX, 2) + 
            Math.pow(touchY - secondLastTapY, 2)
        );
        
        // Check for triple tap - all three taps must be close in position and time
        if (touchEndTime - secondLastTapTime < TRIPLE_TAP_THRESHOLD && 
            distanceFromLastTap < TAP_DISTANCE_THRESHOLD && 
            distanceFromSecondLastTap < TAP_DISTANCE_THRESHOLD) {
            
            // Execute 3D rotation (randomly choose horizontal or vertical)
            if (Math.random() > 0.5) {
                p.rotate3DX();
            } else {
                p.rotate3DY();
            }
            
            // Reset tap tracking after triple tap
            secondLastTapTime = 0;
            lastTapTime = 0;
            return;
        }
        
        // Check for double tap (for hard drop)
        const timeBetweenTaps = touchEndTime - lastTapTime;
        if (timeBetweenTaps < DOUBLE_TAP_THRESHOLD && distanceFromLastTap < TAP_DISTANCE_THRESHOLD) {
            // Track second tap for potential triple tap
            secondLastTapTime = lastTapTime;
            secondLastTapX = lastTapX;
            secondLastTapY = lastTapY;
            
            // Update last tap
            lastTapTime = touchEndTime;
            lastTapX = touchX;
            lastTapY = touchY;
            
            // If we already have a second tap recorded, this is too far from triple tap time
            if (touchEndTime - secondLastTapTime > DOUBLE_TAP_THRESHOLD) {
                p.hardDrop();
                lastTapTime = 0;
                secondLastTapTime = 0;
            }
        } else {
            // Single tap - rotates piece and resets tap tracking
            if (lastTapTime === 0) {
                // First tap
                p.rotate('right');
                lastTapTime = touchEndTime;
                lastTapX = touchX;
                lastTapY = touchY;
            } else {
                // Too far from last tap position - treat as new first tap
                p.rotate('right');
                secondLastTapTime = 0;
                lastTapTime = touchEndTime;
                lastTapX = touchX;
                lastTapY = touchY;
            }
        }
    }
    
    // Reset touch identifier
    touchIdentifier = null;
}

// Create touch instructions overlay
function createTouchControlButtons() {
    // Do not create buttons - using gesture-based controls only
    
    // Create touch instructions overlay
    const touchInstructions = document.createElement('div');
    touchInstructions.className = 'touch-instructions';
    touchInstructions.innerHTML = `
        <h3>Touch Controls</h3>
        <p><b>Swipe left/right:</b> Move piece</p>
        <p><b>Swipe down:</b> Soft drop</p>
        <p><b>Tap anywhere:</b> Rotate right</p>
        <p><b>Double-tap:</b> Hard drop</p>
        <p><b>Triple-tap:</b> 3D rotate</p>
    `;
    document.body.appendChild(touchInstructions);
    
    // Show instructions briefly, then fade out
    setTimeout(() => {
        touchInstructions.classList.add('fade-out');
        setTimeout(() => {
            // Keep element in DOM but hidden, so it can be shown again later
            touchInstructions.style.opacity = '0';
            touchInstructions.style.display = 'none';
        }, 1000);
    }, 5000);
    
    // Add instruction toggle button to score container
    const instructionsBtn = document.createElement('button');
    instructionsBtn.className = 'game-btn instructions-btn';
    instructionsBtn.innerHTML = 'Controls';
    instructionsBtn.addEventListener('click', function() {
        // Show instructions again
        touchInstructions.style.display = 'block';
        touchInstructions.style.opacity = '1';
        touchInstructions.classList.remove('fade-out');
        
        // Fade out after 5 seconds
        setTimeout(() => {
            touchInstructions.classList.add('fade-out');
            setTimeout(() => {
                touchInstructions.style.display = 'none';
            }, 1000);
        }, 5000);
    });
    
    // Add button to score container
    const scoreContainer = document.querySelector('.score-container');
    if (scoreContainer) {
        scoreContainer.appendChild(instructionsBtn);
    }
}

// Set active piece to next piece and create new next piece
function getNextPiece() {
    // Current piece becomes the next piece
    p = nextPiece;
    
    // Generate a new next piece
    nextPiece = randomPiece();
    
    // Draw the next piece in preview
    nextPiece.drawNextPiece();
    
    // Force redraw for mobile to ensure it renders
    if (isMobile || forceMobileControls) {
        setTimeout(() => {
            nextPieceCtx.clearRect(0, 0, nextPieceCanvas.width, nextPieceCanvas.height);
            nextPiece.drawNextPiece();
        }, 50);
    }
}

// Lock the piece in the board and get the next piece
function lockPiece() {
    // Add piece to board
    for (let r = 0; r < p.activeTetromino.length; r++) {
        for (let c = 0; c < p.activeTetromino[r].length; c++) {
            // Skip empty squares
            if (!p.activeTetromino[r][c]) {
                continue;
            }
            
            // Game over when piece is locked at the top
            if (p.y + r < 0) {
                gameOver = true;
                showGameOver();
                return;
            }
            
            // Lock piece
            board[p.y + r][p.x + c] = p.color;
        }
    }
    
    // Check for completed rows
    checkRows();
    
    // Update the score
    updateScore();
    
    // Get next piece
    getNextPiece();
    
    // Reset drop timer
    dropStart = Date.now();
    
    // Play drop sound
    playSound(dropSound);
}

// Generate pieces using 7-bag randomization
function generateBag() {
    // Create array with indices 0-6 (one for each piece type)
    let bag = [0, 1, 2, 3, 4, 5, 6];
    
    // Fisher-Yates shuffle algorithm
    for (let i = bag.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [bag[i], bag[j]] = [bag[j], bag[i]]; // Swap elements
    }
    
    return bag;
}

// Get next piece from the bag
function getNextPieceFromBag() {
    // If the current bag is empty, use the prepared next bag
    if (pieceBag.length === 0) {
        pieceBag = nextBag.slice();
        // Generate new bag for next time
        nextBag = generateBag();
    }
    
    // Take the first piece from the bag
    const pieceIndex = pieceBag.shift();
    const tetromino = PIECES[pieceIndex];
    const color = COLORS[pieceIndex];
    
    // Random rotation/orientation
    const randomIndex = Math.floor(Math.random() * tetromino.length);
    
    return new Piece(tetromino, randomIndex, color);
}

// Start the game
window.onload = function() {
    init();
    update(); // Start the fireworks update loop
    draw();   // Start the drawing loop
    
    // Initialize touch controls if on mobile device
    if (isMobile) {
        initTouchControls();
        touchControls = true;
        
        // Force resize to ensure proper mobile layout
        window.dispatchEvent(new Event('resize'));
    }
}; 