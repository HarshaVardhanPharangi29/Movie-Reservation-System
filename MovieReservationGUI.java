import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.Vector;

public class MovieReservationGUI extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/movie_reservation";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "BangBang@1234";
    
    private Connection connection;
    private JTabbedPane tabbedPane;
    private JTable moviesTable, reservationsTable;
    private DefaultTableModel moviesModel, reservationsModel;
    private JTextField nameField, phoneField, searchField;
    private JComboBox<String> movieCombo;
    private JSpinner seatsSpinner;
    
    public MovieReservationGUI() {
        initDB();
        initGUI();
        loadMovies();
    }
    
    private void initDB() {
        try {
            connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void initGUI() {
        setTitle("🎬 Movie Reservation System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("🎥 Movies", createMoviesPanel());
        tabbedPane.addTab("🎫 Book Tickets", createBookingPanel());
        tabbedPane.addTab("📋 Reservations", createReservationsPanel());
        
        add(tabbedPane);
    }
    
    private JPanel createMoviesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadMovies());
        
        String[] cols = {"ID", "Title", "Genre", "Duration", "Show Time", "Seats", "Price"};
        moviesModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        moviesTable = new JTable(moviesModel);
        moviesTable.setRowHeight(25);
        
        panel.add(refreshBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(moviesTable), BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createBookingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Form fields
        nameField = new JTextField(20);
        phoneField = new JTextField(20);
        movieCombo = new JComboBox<>();
        seatsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        
        // Layout
        addFormField(panel, gbc, 0, "👤 Name:", nameField);
        addFormField(panel, gbc, 1, "📱 Phone:", phoneField);
        addFormField(panel, gbc, 2, "🎬 Movie:", movieCombo);
        addFormField(panel, gbc, 3, "🪑 Seats:", seatsSpinner);
        
        JButton bookBtn = new JButton("🎫 Book Tickets");
        bookBtn.setBackground(new Color(76, 175, 80));
        bookBtn.setForeground(Color.WHITE);
        bookBtn.addActionListener(this::bookTickets);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(20, 10, 10, 10);
        panel.add(bookBtn, gbc);
        
        return panel;
    }
    
    private JPanel createReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchField = new JTextField(15);
        JButton searchBtn = new JButton("🔍 Search");
        searchBtn.addActionListener(e -> loadReservations(searchField.getText().trim()));
        searchPanel.add(new JLabel("📱 Phone:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        
        // Table
        String[] cols = {"ID", "Customer", "Movie", "Seats", "Date", "Total", "Action"};
        reservationsModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return c == 6; } };
        reservationsTable = new JTable(reservationsModel);
        reservationsTable.setRowHeight(25);
        reservationsTable.getColumn("Action").setCellRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JButton btn = new JButton("Cancel");
            btn.setBackground(Color.RED);
            btn.setForeground(Color.WHITE);
            return btn;
        });
        reservationsTable.getColumn("Action").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                JButton btn = new JButton("Cancel");
                btn.setBackground(Color.RED);
                btn.setForeground(Color.WHITE);
                btn.addActionListener(e -> {
                    int id = (Integer) reservationsTable.getValueAt(row, 0);
                    if (JOptionPane.showConfirmDialog(null, "Cancel reservation?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) {
                        cancelReservation(id);
                        stopCellEditing();
                    }
                });
                return btn;
            }
        });
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        return panel;
    }
    
    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }
    
    private void loadMovies() {
        moviesModel.setRowCount(0);
        movieCombo.removeAllItems();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM movies ORDER BY movie_id")) {
            
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("movie_id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("genre"));
                row.add(rs.getInt("duration"));
                row.add(rs.getString("show_time"));
                row.add(rs.getInt("available_seats"));
                row.add(String.format("$%.2f", rs.getDouble("price")));
                moviesModel.addRow(row);
                
                if (rs.getInt("available_seats") > 0) {
                    movieCombo.addItem(rs.getInt("movie_id") + " - " + rs.getString("title") + " (Available: " + rs.getInt("available_seats") + ")");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading movies: " + e.getMessage());
        }
    }
    
    private void bookTickets(ActionEvent e) {
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String selected = (String) movieCombo.getSelectedItem();
        int seats = (Integer) seatsSpinner.getValue();
        
        if (name.isEmpty() || phone.isEmpty() || selected == null) {
            JOptionPane.showMessageDialog(this, "Please fill all fields!");
            return;
        }
        
        int movieId = Integer.parseInt(selected.split(" - ")[0]);
        
        try {
            // Check availability
            PreparedStatement checkStmt = connection.prepareStatement("SELECT available_seats, price FROM movies WHERE movie_id = ?");
            checkStmt.setInt(1, movieId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("available_seats") >= seats) {
                double total = rs.getDouble("price") * seats;
                
                // Book reservation
                PreparedStatement bookStmt = connection.prepareStatement(
                    "INSERT INTO reservations (customer_name, phone_number, movie_id, num_seats, reservation_date, total_amount) VALUES (?, ?, ?, ?, NOW(), ?)");
                bookStmt.setString(1, name);
                bookStmt.setString(2, phone);
                bookStmt.setInt(3, movieId);
                bookStmt.setInt(4, seats);
                bookStmt.setDouble(5, total);
                bookStmt.executeUpdate();
                
                // Update seats
                PreparedStatement updateStmt = connection.prepareStatement("UPDATE movies SET available_seats = available_seats - ? WHERE movie_id = ?");
                updateStmt.setInt(1, seats);
                updateStmt.setInt(2, movieId);
                updateStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, String.format("🎉 Booking Successful!\n\nCustomer: %s\nSeats: %d\nTotal: $%.2f", name, seats, total));
                
                // Clear form
                nameField.setText("");
                phoneField.setText("");
                seatsSpinner.setValue(1);
                loadMovies();
                
            } else {
                JOptionPane.showMessageDialog(this, "Not enough seats available!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Booking failed: " + ex.getMessage());
        }
    }
    
    private void loadReservations(String phone) {
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter phone number!");
            return;
        }
        
        reservationsModel.setRowCount(0);
        
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT r.reservation_id, r.customer_name, m.title, r.num_seats, r.reservation_date, r.total_amount " +
            "FROM reservations r JOIN movies m ON r.movie_id = m.movie_id WHERE r.phone_number = ? ORDER BY r.reservation_date DESC")) {
            
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            
            boolean found = false;
            while (rs.next()) {
                found = true;
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("reservation_id"));
                row.add(rs.getString("customer_name"));
                row.add(rs.getString("title"));
                row.add(rs.getInt("num_seats"));
                row.add(rs.getTimestamp("reservation_date").toString());
                row.add(String.format("$%.2f", rs.getDouble("total_amount")));
                row.add("Cancel");
                reservationsModel.addRow(row);
            }
            
            if (!found) {
                JOptionPane.showMessageDialog(this, "No reservations found for: " + phone);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading reservations: " + e.getMessage());
        }
    }
    
    private void cancelReservation(int reservationId) {
        try {
            // Get details
            PreparedStatement getStmt = connection.prepareStatement("SELECT movie_id, num_seats FROM reservations WHERE reservation_id = ?");
            getStmt.setInt(1, reservationId);
            ResultSet rs = getStmt.executeQuery();
            
            if (rs.next()) {
                int movieId = rs.getInt("movie_id");
                int seats = rs.getInt("num_seats");
                
                // Delete reservation
                PreparedStatement delStmt = connection.prepareStatement("DELETE FROM reservations WHERE reservation_id = ?");
                delStmt.setInt(1, reservationId);
                delStmt.executeUpdate();
                
                // Restore seats
                PreparedStatement restoreStmt = connection.prepareStatement("UPDATE movies SET available_seats = available_seats + ? WHERE movie_id = ?");
                restoreStmt.setInt(1, seats);
                restoreStmt.setInt(2, movieId);
                restoreStmt.executeUpdate();
                
                JOptionPane.showMessageDialog(this, "Reservation cancelled successfully!");
                loadMovies();
                reservationsModel.setRowCount(0);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error cancelling reservation: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MovieReservationGUI().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage());
            }
        });
    }
}