# Cyber Legacy

<img width="1920" height="1080" alt="Cyber Legacy Gameplay" src="https://github.com/user-attachments/assets/d055c615-faa0-4aac-be52-6a5b57920dd6" />
<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Swing-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Graphics2D-8E24AA?style=for-the-badge">
  <img src="https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white">
  <img src="https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white">
  <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white">
</p>

Cyber Legacy is a 2D action game developed entirely from scratch in Java using Swing and Graphics2D.

The project was created as a practical learning experience focused on game development, object-oriented programming, software architecture, and data persistence. Instead of relying on a game engine, core systems are implemented manually.

## Highlights

* Built entirely from scratch
* No game engine used
* Custom game loop
* Entity-based architecture
* Enemy AI
* Dynamic camera system
* SQLite database integration
* Persistent player statistics
* Match history tracking

## Features

### Gameplay

* Multiple playable classes
* Experience (XP) and leveling system
* Enemy wave progression
* Enemy AI
* Dynamic camera and zoom
* Collision detection
* Sprite animations
* Particle effects
* Floating combat text
* Sound effects and background music
* Game state management

### Technical Features

* Custom rendering pipeline using Graphics2D
* Resource management system
* Entity management system
* SQLite persistence layer
* Automatic database initialization
* Automatic statistics tracking
* Score history storage

## Database

Cyber Legacy uses SQLite to persist player progression and game statistics.

### Stored Information

* Player profile
* Total kills
* Best score
* Total experience
* Match history
* Play sessions
* Achievement structure for future updates

### Database Tables

#### players

Stores player progression and statistics.

#### scores

Stores match history, score records, kills, levels, and phases reached.

#### achievements

Prepared for future achievement system implementation.

### Automatic Features

* Database creation on first launch
* Table creation on startup
* Automatic score saving
* Automatic player tracking
* Persistent statistics between sessions

## Architecture

The project applies several software engineering concepts:

* Object-Oriented Programming (OOP)
* Separation of Concerns
* Entity-Based Design
* State Management
* Resource Management
* Data Persistence
* Collision Handling
* Custom Rendering System

## Tech Stack

* Java 21
* Java Swing
* Graphics2D
* SQLite
* SQLite JDBC
* Git
* GitHub
* IntelliJ IDEA

## Controls

| Action            | Key               |
| ----------------- | ----------------- |
| Move              | W A S D           |
| Aim               | Mouse             |
| Attack            | Left Mouse Button |
| Interact / Select | Space / Enter     |

## Project Structure

```text
CyberLegacy/
├── assets/
│   ├── audio/
│   ├── sprites/
│   └── fonts/
│
├── src/
│   └── ...
│
├── lib/
│
├── cyberlegacy/
│   └── data/
│       └── cyberlegacy.db
│
└── docs/
```

## Running the Project

### Requirements

* Java 21 or newer
* IntelliJ IDEA (recommended)

### Clone the Repository

```bash
git clone https://github.com/thurdass/CyberLegacy.git
```

### Run

Open the project in IntelliJ IDEA and run the main game class.

The SQLite database is automatically created during the first execution.

## Roadmap

* [x] SQLite database integration
* [x] Persistent player statistics
* [x] Match history tracking
* [ ] Achievement system
* [ ] Boss battles
* [ ] Inventory system
* [ ] Equipment system
* [ ] New maps
* [ ] Additional enemy types
* [ ] Improved enemy AI
* [ ] Performance optimizations

## Development Status

Cyber Legacy is an active project and continues to evolve as new mechanics, systems, and improvements are implemented.

The goal is to deepen knowledge in Java, software architecture, game development, and backend concepts through a real-world project.

## Author

**Arthur Almeida**

* GitHub: https://github.com/thurdass
* LinkedIn: https://www.linkedin.com/in/arthur-almeida-643a833b1/
