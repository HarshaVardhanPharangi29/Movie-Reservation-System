import java.sql.*;

public class DatabaseSetup {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "movie_reservation";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "BangBang@1234";
    
    public static void main(String[] args) {
        DatabaseSetup setup = new DatabaseSetup();
        setup.createDatabase();
        setup.createTables();
        setup.insertSampleData();
        System.out.println("Database setup completed successfully!");
        
        // Show completion dialog
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(null, 
                "✅ Database Setup Completed!\n\n" +
                "• Database 'movie_reservation' created\n" +
                "• Tables created with sample data\n" +
                "• Ready to run MovieReservationGUI\n\n" +
                "You can now run the GUI application!",
                "Setup Complete", 
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });
    }
    
    private void createDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Create database if it doesn't exist
            String sql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME;
            stmt.executeUpdate(sql);
            System.out.println("Database created successfully!");
            
        } catch (SQLException e) {
            System.err.println("Error creating database: " + e.getMessage());
        }
    }
    
    private void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL + DB_NAME, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Create movies table
            String createMoviesTable = """
                CREATE TABLE IF NOT EXISTS movies (
                    movie_id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(100) NOT NULL,
                    genre VARCHAR(50) NOT NULL,
                    duration INT NOT NULL,
                    show_time VARCHAR(20) NOT NULL,
                    total_seats INT DEFAULT 100,
                    available_seats INT DEFAULT 100,
                    price DECIMAL(10,2) DEFAULT 10.00
                )
                """;
            stmt.executeUpdate(createMoviesTable);
            System.out.println("Movies table created successfully!");
            
            // Create reservations table
            String createReservationsTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
                    customer_name VARCHAR(100) NOT NULL,
                    phone_number VARCHAR(15) NOT NULL,
                    movie_id INT NOT NULL,
                    num_seats INT NOT NULL,
                    reservation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_amount DECIMAL(10,2),
                    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE
                )
                """;
            stmt.executeUpdate(createReservationsTable);
            System.out.println("Reservations table created successfully!");
            
            // Create customers table (optional for future enhancements)
            String createCustomersTable = """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE,
                    phone_number VARCHAR(15) UNIQUE NOT NULL,
                    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            stmt.executeUpdate(createCustomersTable);
            System.out.println("Customers table created successfully!");
            
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }
    
    private void insertSampleData() {
        try (Connection conn = DriverManager.getConnection(DB_URL + DB_NAME, USERNAME, PASSWORD)) {
            
            // Check if movies already exist
            String checkSql = "SELECT COUNT(*) FROM movies";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 ResultSet rs = checkStmt.executeQuery()) {
                
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Sample data already exists, skipping insertion.");
                    return;
                }
            }
            
            // Insert sample movies
            String insertMoviesSql = """
                INSERT INTO movies (title, genre, duration, show_time, total_seats, available_seats, price) VALUES
                ('Avatar: The Way of Water', 'Sci-Fi', 192, '10:00 AM', 150, 150, 15.00),
                ('Top Gun: Maverick', 'Action', 131, '01:00 PM', 120, 120, 12.50),
                ('Black Panther: Wakanda Forever', 'Action', 161, '04:00 PM', 100, 100, 14.00),
                ('The Batman', 'Action', 176, '07:00 PM', 180, 180, 13.50),
                ('Spider-Man: No Way Home', 'Action', 148, '10:00 PM', 200, 200, 16.00),
                ('Dune', 'Sci-Fi', 155, '11:00 AM', 140, 140, 13.00),
                ('No Time to Die', 'Action', 163, '02:30 PM', 130, 130, 12.00),
                ('The Matrix Resurrections', 'Sci-Fi', 148, '06:00 PM', 110, 110, 14.50),
                ('Fast & Furious 9', 'Action', 143, '09:30 PM', 160, 160, 11.50),
                ('Wonder Woman 1984', 'Action', 151, '12:00 PM', 125, 125, 13.75)
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(insertMoviesSql)) {
                stmt.executeUpdate();
                System.out.println("Sample movies inserted successfully!");
            }
            
            // Insert sample customers
            String insertCustomersSql = """
                INSERT INTO customers (name, email, phone_number) VALUES
                ('John Doe', 'john.doe@email.com', '9876543210'),
                ('Jane Smith', 'jane.smith@email.com', '8765432109'),
                ('Mike Johnson', 'mike.johnson@email.com', '7654321098'),
                ('Sarah Wilson', 'sarah.wilson@email.com', '6543210987'),
                ('David Brown', 'david.brown@email.com', '5432109876')
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(insertCustomersSql)) {
                stmt.executeUpdate();
                System.out.println("Sample customers inserted successfully!");
            }
            
            // Insert sample reservations
            String insertReservationsSql = """
                INSERT INTO reservations (customer_name, phone_number, movie_id, num_seats, total_amount) VALUES
                ('John Doe', '9876543210', 1, 2, 30.00),
                ('Jane Smith', '8765432109', 3, 4, 56.00),
                ('Mike Johnson', '7654321098', 5, 3, 48.00)
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(insertReservationsSql)) {
                stmt.executeUpdate();
                // Update available seats for the reserved movies
                updateSeatsAfterReservation(conn, 1, 2);
                updateSeatsAfterReservation(conn, 3, 4);
                updateSeatsAfterReservation(conn, 5, 3);
                System.out.println("Sample reservations inserted successfully!");
            }
            
        } catch (SQLException e) {
            System.err.println("Error inserting sample data: " + e.getMessage());
        }
    }
    
    private void updateSeatsAfterReservation(Connection conn, int movieId, int reservedSeats) throws SQLException {
        String updateSql = "UPDATE movies SET available_seats = available_seats - ? WHERE movie_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, reservedSeats);
            stmt.setInt(2, movieId);
            stmt.executeUpdate();
        }
    }
    
    // Method to reset database (useful for testing)
    public void resetDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL + DB_NAME, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate("DROP DATABASE IF EXISTS " + DB_NAME);
            System.out.println("Database reset completed!");
            
        } catch (SQLException e) {
            System.err.println("Error resetting database: " + e.getMessage());
        }
    }
    
    // Method to display current database status
    public void displayDatabaseStatus() {
        try (Connection conn = DriverManager.getConnection(DB_URL + DB_NAME, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("\n=== DATABASE STATUS ===");
            
            // Count movies
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM movies");
            if (rs.next()) {
                System.out.println("Total Movies: " + rs.getInt(1));
            }
            
            // Count reservations
            rs = stmt.executeQuery("SELECT COUNT(*) FROM reservations");
            if (rs.next()) {
                System.out.println("Total Reservations: " + rs.getInt(1));
            }
            
            // Count customers
            rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next()) {
                System.out.println("Total Customers: " + rs.getInt(1));
            }
            
            // Show available vs total seats
            rs = stmt.executeQuery("SELECT SUM(available_seats), SUM(total_seats) FROM movies");
            if (rs.next()) {
                System.out.println("Available Seats: " + rs.getInt(1) + "/" + rs.getInt(2));
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking database status: " + e.getMessage());
        }
    }
}
