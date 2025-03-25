# 3D Tetris

A Tetris clone with 3D rotation capabilities, allowing pieces to be rotated in three dimensions.

## How to Play

1. Open `index.html` in your web browser to start the game.
2. Use the following controls to play:

### Controls
- **A/Left Arrow**: Move left
- **D/Right Arrow**: Move right  
- **S/Down Arrow**: Move down
- **Q**: Rotate left (standard rotation)
- **E**: Rotate right (standard rotation)
- **W**: Horizontal 3D rotation
- **X**: Vertical 3D rotation
- **Space**: Hard drop
- **P**: Pause/resume game

## Game Features

- Standard Tetris mechanics with line clearing and scoring
- Unique 3D rotation abilities that let you rotate pieces horizontally and vertically
- Increasing difficulty as you level up
- Score tracking based on lines cleared and level

## Implementation Details

This game is built using vanilla HTML, CSS and JavaScript with HTML5 Canvas for rendering.

The 3D aspect is simulated by having additional piece orientations that represent what the pieces would look like when rotated in 3D space. This gives the illusion of 3D movement while maintaining the 2D gameplay that makes Tetris fun and accessible. 