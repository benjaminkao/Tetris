# Tetris Game and Bot

## Motivation
I wanted to learn how to create a game loop and improve my collision detection coding functions. Also, I wanted to learn how to do work in a background thread and have that work be shown on the UI.

## Built With:
- Made in JavaFX
- Tetris bot is heuristic based and runs on a separate thread.
  - Tetris bot demonstrates multithreading



## Latest Update:

### August 20, 2020
User can play now; Fixed some bugs
- Before, user could not play because bot was always on. Added checkbox to turn bot on/off so now user can play.
- Fixed bug where tetrominoes would fall faster every time a new game was started even after level was reset.

## Previous Updates:

### August 16, 2020
Improved Bot; Added Leveling; Updated UI
- Improved bot decision making
  - Bot now takes into consideration holding
- Added leveling system so the more rows cleared, the faster the tetrominoes fall
- Added scoring label
- Added "Play Again" feature


### August 15, 2020
Bare Bones Tetris Game and Bot Completed
- Tetris game is working, but have not implemented scoring and UI is basic
- First version of tetris bot is completed, but bot is not very good at the game
