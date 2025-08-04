# MsPacMan Agent Lab

This project implements a custom AI agent for Ms. Pac-Man using Java.  
The agent uses state-based logic to prioritize:
- Eating pills
- Avoiding ghosts
- Using power pills strategically

## How to Run
1. Compile: `javac src/pacman/MsPacManAgent.java`
2. Run with the game engine: `java -cp .:lib/pacman-core.jar pacman.MsPacManAgent`

## Features
- BFS/UCS pathfinding
- Ghost and junction awareness
- Pill prioritization logic
