package com.thurdas.cyberlegacy.database;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_PATH = "cyberlegacy/data/cyberlegacy.db";
    private Connection connection;

    public DatabaseManager() {
        initDatabase();
    }

    private void initDatabase() {
        try {
            java.io.File dataDir = new java.io.File("cyberlegacy/data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            // Load explicitly so a misconfigured runtime classpath is reported clearly.
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + DB_PATH;
            connection = DriverManager.getConnection(url);
            createTables();
            System.out.println("Database initialized successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found. Add lib/sqlite-jdbc-3.44.0.0.jar to the runtime classpath.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            String createPlayersTable = """
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    class TEXT NOT NULL,
                    level INTEGER DEFAULT 1,
                    kills INTEGER DEFAULT 0,
                    max_score INTEGER DEFAULT 0,
                    total_xp INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createScoresTable = """
                CREATE TABLE IF NOT EXISTS scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id INTEGER NOT NULL,
                    score INTEGER NOT NULL,
                    kills INTEGER NOT NULL,
                    level INTEGER NOT NULL,
                    phase INTEGER NOT NULL,
                    class TEXT NOT NULL,
                    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_id) REFERENCES players(id)
                )
                """;

            String createAchievementsTable = """
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_id INTEGER NOT NULL,
                    achievement_name TEXT NOT NULL,
                    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (player_id) REFERENCES players(id),
                    UNIQUE(player_id, achievement_name)
                )
                """;

            stmt.execute(createPlayersTable);
            stmt.execute(createScoresTable);
            stmt.execute(createAchievementsTable);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    public boolean playerExists(String name) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM players WHERE name = '" + name + "'");
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking player existence: " + e.getMessage());
            return false;
        }
    }

    public int getOrCreatePlayer(String name, String playerClass) {
        if (playerExists(name)) {
            return getPlayerId(name);
        }

        try (Statement stmt = connection.createStatement()) {
            String query = String.format(
                "INSERT INTO players (name, class) VALUES ('%s', '%s')",
                name, playerClass
            );
            stmt.execute(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error creating player: " + e.getMessage());
        }
        return -1;
    }

    public int getPlayerId(String name) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT id FROM players WHERE name = '" + name + "'");
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting player ID: " + e.getMessage());
        }
        return -1;
    }

    public void saveGameScore(int playerId, int score, int kills, int level, int phase, String playerClass) {
        try (Statement stmt = connection.createStatement()) {
            String query = String.format(
                "INSERT INTO scores (player_id, score, kills, level, phase, class) VALUES (%d, %d, %d, %d, %d, '%s')",
                playerId, score, kills, level, phase, playerClass
            );
            stmt.execute(query);

            String updateQuery = String.format(
                "UPDATE players SET max_score = MAX(max_score, %d), kills = MAX(kills, %d), total_xp = total_xp + %d, last_played = CURRENT_TIMESTAMP WHERE id = %d",
                score, kills, score, playerId
            );
            stmt.execute(updateQuery);
        } catch (SQLException e) {
            System.err.println("Error saving game score: " + e.getMessage());
        }
    }

    public PlayerStats getPlayerStats(String name) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT id, name, class, level, kills, max_score, total_xp FROM players WHERE name = '" + name + "'"
            );
            if (rs.next()) {
                return new PlayerStats(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("class"),
                    rs.getInt("level"),
                    rs.getInt("kills"),
                    rs.getInt("max_score"),
                    rs.getInt("total_xp")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting player stats: " + e.getMessage());
        }
        return null;
    }

    public void addAchievement(int playerId, String achievementName) {
        try (Statement stmt = connection.createStatement()) {
            String query = String.format(
                "INSERT OR IGNORE INTO achievements (player_id, achievement_name) VALUES (%d, '%s')",
                playerId, achievementName
            );
            stmt.execute(query);
        } catch (SQLException e) {
            System.err.println("Error adding achievement: " + e.getMessage());
        }
    }

    public int getTopScore() {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT MAX(score) as top_score FROM scores");
            if (rs.next()) {
                return rs.getInt("top_score");
            }
        } catch (SQLException e) {
            System.err.println("Error getting top score: " + e.getMessage());
        }
        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    public static class PlayerStats {
        public int id;
        public String name;
        public String playerClass;
        public int level;
        public int kills;
        public int maxScore;
        public int totalXp;

        public PlayerStats(int id, String name, String playerClass, int level, int kills, int maxScore, int totalXp) {
            this.id = id;
            this.name = name;
            this.playerClass = playerClass;
            this.level = level;
            this.kills = kills;
            this.maxScore = maxScore;
            this.totalXp = totalXp;
        }

        @Override
        public String toString() {
            return String.format("Player: %s | Class: %s | Level: %d | Kills: %d | Best Score: %d",
                name, playerClass, level, kills, maxScore);
        }
    }
}
