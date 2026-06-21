# Who Am I Game

A coursework project for the "Development of Client-Server Applications on Java" course.

## Project Team (Виконавці роботи)

- **Вус Павло** — ІПЗ-2, група №4
- **Літвінчук Роман** — ІПЗ-2, група №3

## Project Idea

"Who Am I?" is an online client-server application implemented in pure Java, where two players participate in a text-based character guessing game. One player receives a hidden character, and the other asks questions that can only be answered with "Yes", "No", or "Partially". The goal is to guess the character within a limited number of questions.

## Key Features

The project encompasses four mandatory technical components: network interaction, encryption with a custom network protocol, multithreading, and database management.

### Player Capabilities
- **Authentication & Accounts**: Registration, login, and personal statistics tracking (wins, losses, average questions).
- **Game Rooms**: Create game rooms with unique codes, share them to invite friends, or join existing rooms.
- **Gameplay Mechanics**: Automated role assignment (Riddler and Guesser). Guessers ask text questions, and Riddlers answer. Guessers have a limit of 20 questions per round and up to 3 final attempts to guess the character's name.
- **Character Bank**: Characters are stored in the database and categorized (Movies & TV Series, Video Games, Books & Cartoons, Real People, Animals). Players can also suggest custom characters.

### Administrator Capabilities
- **Admin Panel**: Monitor active network connections and ongoing game sessions.
- **Moderation**: Forcefully terminate any active game or block a player's account.
- **Content Management**: Add, edit, or delete characters in the global character bank.

## Architecture and Technologies

- **Programming Language**: Java 24
- **Network Interaction**: `java.net.Socket`, `java.net.ServerSocket`, multithreading (`ExecutorService`).
- **Database**: PostgreSQL (via JDBC and HikariCP connection pool).
- **Data Protocol & Security**: Custom binary protocol. Sensitive payload data (like character names) is encrypted using **AES-128**. User passwords are hashed using **Bcrypt**. Session management is implemented via **JWT** (JSON Web Tokens).
- **Build System**: Maven (multi-module project: `protocol`, `server`, `client`).