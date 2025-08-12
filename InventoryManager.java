// InventoryManager.java
// Requires: DBConnection.getConnection() in same project
// Compile: javac -cp ".:lib/*" InventoryManager.java
// Run:     java -cp ".:lib/*" InventoryManager

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class InventoryManager {

    private static JFrame frame;
    private static CardLayout cardLayout;
    private static JPanel rootPanel;

    // Hardcoded users
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";
    private static final String CLIENT_USER = "client";
    private static final String CLIENT_PASS = "client123";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Inventory Management System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);

            cardLayout = new CardLayout();
            rootPanel = new JPanel(cardLayout);

            rootPanel.add(createLoginPanel(), "login");
            rootPanel.add(AdminPanel.getPanel(), "admin");
            rootPanel.add(ClientPanel.getPanel(), "client");

            frame.setContentPane(rootPanel);
            cardLayout.show(rootPanel, "login");
            frame.setVisible(true);
        });
    }

    // ---------- LOGIN ----------
    private static JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(34, 34, 34));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Inventory Management — Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.WHITE);
        JTextField userField = new JTextField(18);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.WHITE);
        JPasswordField passField = new JPasswordField(18);

        JButton loginBtn = new JButton("Login");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        p.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1;
        p.add(userLabel, gbc);
        gbc.gridx = 1;
        p.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        p.add(passLabel, gbc);
        gbc.gridx = 1;
        p.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        p.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String pw = new String(passField.getPassword()).trim();
            if (ADMIN_USER.equals(u) && ADMIN_PASS.equals(pw)) {
                AdminPanel.refreshInventory();
                cardLayout.show(rootPanel, "admin");
            } else if (CLIENT_USER.equals(u) && CLIENT_PASS.equals(pw)) {
                ClientPanel.refreshInventory();
                cardLayout.show(rootPanel, "client");
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid credentials", "Login failed", JOptionPane.ERROR_MESSAGE);
            }
            userField.setText("");
            passField.setText("");
        });

        return p;
    }

    // ---------- ADMIN PANEL (with download functionality) ----------
    static class AdminPanel {
        private static JPanel panel;
        private static DefaultTableModel inventoryModel;
        private static JTable inventoryTable;
        private static JTextField nameField, countField, priceField;
        private static JComboBox<String> categoryBox;

        static JPanel getPanel() {
            if (panel == null) panel = createPanel();
            return panel;
        }

        private static JPanel createPanel() {
            panel = new JPanel(new BorderLayout(10,10));
            panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JLabel header = new JLabel("Admin Panel — Manage Inventory", SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 20));
            panel.add(header, BorderLayout.NORTH);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            split.setDividerLocation(360);

            // left form
            JPanel left = new JPanel(new GridBagLayout());
            left.setBorder(BorderFactory.createTitledBorder("Add New Item"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0; left.add(new JLabel("Name:"), gbc);
            nameField = new JTextField(16); gbc.gridx = 1; left.add(nameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1; left.add(new JLabel("Category:"), gbc);
            categoryBox = new JComboBox<>(new String[]{"Food","Stationary","Kitchen","Bathroom","Other"});
            gbc.gridx = 1; left.add(categoryBox, gbc);

            gbc.gridx = 0; gbc.gridy = 2; left.add(new JLabel("Count:"), gbc);
            countField = new JTextField(8); gbc.gridx = 1; left.add(countField, gbc);

            gbc.gridx = 0; gbc.gridy = 3; left.add(new JLabel("Price:"), gbc);
            priceField = new JTextField(8); gbc.gridx = 1; left.add(priceField, gbc);

            gbc.gridx = 0; gbc.gridy = 4; left.add(new JLabel("Min Stock:"), gbc);
            JTextField minStockField = new JTextField(8); 
            minStockField.setText("5"); // Default minimum stock
            gbc.gridx = 1; left.add(minStockField, gbc);

            gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
            JButton addBtn = new JButton("Add Item");
            left.add(addBtn, gbc);

            split.setLeftComponent(left);

            // right table
            JPanel right = new JPanel(new BorderLayout());
            right.setBorder(BorderFactory.createTitledBorder("Inventory (editable)"));

            // Add low stock alert panel
            JPanel alertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            alertPanel.setBackground(new Color(255, 245, 245)); // Light red background
            JLabel alertLabel = new JLabel("⚠️ Low Stock Alerts: Loading...");
            alertLabel.setForeground(new Color(180, 0, 0)); // Dark red text
            alertLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            alertPanel.add(alertLabel);
            alertPanel.setVisible(false); // Initially hidden
            right.add(alertPanel, BorderLayout.NORTH);

            inventoryModel = new DefaultTableModel();
            inventoryTable = new JTable(inventoryModel) {
                public boolean isCellEditable(int row, int col) {
                    return col != 0; // keep ID read-only
                }
                
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component comp = super.prepareRenderer(renderer, row, column);
                    
                    // Check if this row has low stock
                    try {
                        if (getColumnCount() >= 5) { // Make sure we have enough columns
                            Object countObj = getValueAt(row, 3); // Count column
                            Object minStockObj = getValueAt(row, 5); // Min Stock column (if exists)
                            
                            if (countObj != null && minStockObj != null) {
                                int currentStock = ((Number) countObj).intValue();
                                int minStock = ((Number) minStockObj).intValue();
                                
                                if (currentStock <= minStock) {
                                    comp.setBackground(new Color(255, 230, 230)); // Light red
                                    comp.setForeground(new Color(150, 0, 0)); // Dark red text
                                } else {
                                    comp.setBackground(getBackground());
                                    comp.setForeground(getForeground());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // If there's any error, use default colors
                        comp.setBackground(getBackground());
                        comp.setForeground(getForeground());
                    }
                    
                    return comp;
                }
            };
            inventoryTable.setRowHeight(24);
            right.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

            split.setRightComponent(right);
            panel.add(split, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton deleteBtn = new JButton("Delete Selected");
            JButton downloadBtn = new JButton("Download Inventory");
            JButton refreshBtn = new JButton("Refresh");
            JButton logoutBtn = new JButton("Logout");
            bottom.add(deleteBtn);
            bottom.add(downloadBtn);
            bottom.add(refreshBtn);
            bottom.add(logoutBtn);
            panel.add(bottom, BorderLayout.SOUTH);

            // actions
            addBtn.addActionListener(e -> {
                String name = nameField.getText().trim().toLowerCase(); // Convert to lowercase
                String category = (String) categoryBox.getSelectedItem();
                String c = countField.getText().trim();
                String p = priceField.getText().trim();
                String minStock = minStockField.getText().trim();
                if (name.isEmpty() || c.isEmpty() || p.isEmpty() || minStock.isEmpty()) { 
                    JOptionPane.showMessageDialog(frame, "Fill all fields"); return; 
                }
                try {
                    int count = Integer.parseInt(c);
                    double price = Double.parseDouble(p);
                    int minStockVal = Integer.parseInt(minStock);
                    if (minStockVal < 0) {
                        JOptionPane.showMessageDialog(frame, "Minimum stock cannot be negative"); return;
                    }
                    try (Connection conn = DBConnection.getConnection()) {
                        // Check for existing item with same name (lowercase), category, and price
                        String check = "SELECT item_count FROM inventory WHERE LOWER(item_name) = ? AND item_category = ? AND item_price = ?";
                        PreparedStatement ps = conn.prepareStatement(check);
                        ps.setString(1, name); // name is already lowercase
                        ps.setString(2, category);
                        ps.setDouble(3, price);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            int exist = rs.getInt("item_count");
                            int newCount = exist + count;
                            // Update the existing item (keep the original name casing in DB, but match by lowercase)
                            String upd = "UPDATE inventory SET item_count = ?, min_stock = ? WHERE LOWER(item_name) = ? AND item_category = ? AND item_price = ?";
                            PreparedStatement ps2 = conn.prepareStatement(upd);
                            ps2.setInt(1, newCount);
                            ps2.setInt(2, minStockVal);
                            ps2.setString(3, name);
                            ps2.setString(4, category);
                            ps2.setDouble(5, price);
                            ps2.executeUpdate();
                            JOptionPane.showMessageDialog(frame, "Updated count to " + newCount + " for existing item");
                        } else {
                            // Insert new item with lowercase name
                            String ins = "INSERT INTO inventory (item_name,item_category,item_count,item_price,min_stock) VALUES (?,?,?,?,?)";
                            PreparedStatement ps2 = conn.prepareStatement(ins);
                            ps2.setString(1, name); // Store as lowercase
                            ps2.setString(2, category);
                            ps2.setInt(3, count); 
                            ps2.setDouble(4, price);
                            ps2.setInt(5, minStockVal);
                            ps2.executeUpdate();
                            JOptionPane.showMessageDialog(frame, "Item '" + name + "' added successfully");
                        }
                    }
                    nameField.setText(""); countField.setText(""); priceField.setText(""); minStockField.setText("5");
                    refreshInventory();
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(frame, "Count/Price/Min Stock must be numbers");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "DB error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // table changes -> push to DB
            inventoryModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int col = e.getColumn();
                    if (row < 0 || col < 0) return;
                    Object idObj = inventoryModel.getValueAt(row, 0);
                    if (idObj == null) return;
                    int id = ((Number) idObj).intValue();
                    String colName = inventoryModel.getColumnName(col);
                    Object value = inventoryModel.getValueAt(row, col);
                    String dbCol = null;
                    if ("Name".equals(colName)) {
                        dbCol = "item_name";
                        // Convert name to lowercase
                        if (value != null) {
                            value = value.toString().toLowerCase();
                            inventoryModel.setValueAt(value, row, col); // Update the display
                        }
                    }
                    else if ("Category".equals(colName)) dbCol = "item_category";
                    else if ("Count".equals(colName)) dbCol = "item_count";
                    else if ("Price".equals(colName)) dbCol = "item_price";
                    else if ("Min Stock".equals(colName)) dbCol = "min_stock";
                    if (dbCol != null) {
                        try (Connection conn = DBConnection.getConnection()) {
                            String sql = "UPDATE inventory SET " + dbCol + " = ? WHERE item_id = ?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            if ("Count".equals(colName) || "Min Stock".equals(colName)) ps.setInt(1, Integer.parseInt(value.toString()));
                            else if ("Price".equals(colName)) ps.setDouble(1, Double.parseDouble(value.toString()));
                            else ps.setString(1, value.toString());
                            ps.setInt(2, id);
                            ps.executeUpdate();
                            // Refresh to update low stock highlighting
                            SwingUtilities.invokeLater(() -> inventoryTable.repaint());
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Update failed: " + ex.getMessage());
                            refreshInventory();
                        }
                    }
                }
            });

            deleteBtn.addActionListener(e -> {
                int selectedRow = inventoryTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(frame, "Please select an item to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Get the item details for confirmation
                Object idObj = inventoryModel.getValueAt(selectedRow, 0);
                Object nameObj = inventoryModel.getValueAt(selectedRow, 1);
                Object categoryObj = inventoryModel.getValueAt(selectedRow, 2);
                Object countObj = inventoryModel.getValueAt(selectedRow, 3);
                
                if (idObj == null) return;
                
                int itemId = ((Number) idObj).intValue();
                String itemName = nameObj != null ? nameObj.toString() : "Unknown";
                String category = categoryObj != null ? categoryObj.toString() : "Unknown";
                int count = countObj != null ? ((Number) countObj).intValue() : 0;
                
                // Confirmation dialog
                int confirm = JOptionPane.showConfirmDialog(frame, 
                    String.format("Are you sure you want to DELETE this item?\n\nID: %d\nName: %s\nCategory: %s\nCount: %d\n\nThis action cannot be undone!", 
                        itemId, itemName, category, count),
                    "Confirm Delete", 
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    try (Connection conn = DBConnection.getConnection()) {
                        String deleteSql = "DELETE FROM inventory WHERE item_id = ?";
                        PreparedStatement ps = conn.prepareStatement(deleteSql);
                        ps.setInt(1, itemId);
                        
                        int rowsAffected = ps.executeUpdate();
                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(frame, 
                                String.format("Item '%s' deleted successfully!", itemName),
                                "Delete Successful", 
                                JOptionPane.INFORMATION_MESSAGE);
                            refreshInventory(); // Refresh the table
                        } else {
                            JOptionPane.showMessageDialog(frame, 
                                "Failed to delete item. It may have already been deleted.",
                                "Delete Failed", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, 
                            "Database error while deleting: " + ex.getMessage(),
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            });

            // Download Inventory Button Action
            downloadBtn.addActionListener(e -> downloadInventory());

            refreshBtn.addActionListener(e -> refreshInventory());
            logoutBtn.addActionListener(e -> cardLayout.show(rootPanel, "login"));

            refreshInventory();
            return panel;
        }

        // Method to download inventory data as CSV
        private static void downloadInventory() {
            try {
                // Generate filename with timestamp
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
                String timestamp = now.format(formatter);
                String fileName = "inventory_" + timestamp + ".csv";
                
                // Create file chooser to let user select save location
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new java.io.File(fileName));
                fileChooser.setDialogTitle("Save Inventory Report");
                
                // Add CSV filter
                javax.swing.filechooser.FileNameExtensionFilter csvFilter = 
                    new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv");
                fileChooser.setFileFilter(csvFilter);
                
                int userSelection = fileChooser.showSaveDialog(frame);
                
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    java.io.File fileToSave = fileChooser.getSelectedFile();
                    
                    // Ensure .csv extension
                    if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                        fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".csv");
                    }
                    
                    // Fetch fresh data from database
                    try (Connection conn = DBConnection.getConnection();
                         FileWriter writer = new FileWriter(fileToSave)) {
                        
                        // Write CSV header
                        writer.append("Item ID,Item Name,Category,Stock Count,Unit Price,Min Stock,Total Value,Stock Status\n");
                        
                        String query = "SELECT item_id, item_name, item_category, item_count, item_price, COALESCE(min_stock, 5) as min_stock FROM inventory ORDER BY item_id";
                        PreparedStatement ps = conn.prepareStatement(query);
                        ResultSet rs = ps.executeQuery();
                        
                        double grandTotal = 0.0;
                        int totalItems = 0;
                        int totalStock = 0;
                        int lowStockItems = 0;
                        
                        // Write data rows
                        while (rs.next()) {
                            int id = rs.getInt("item_id");
                            String name = rs.getString("item_name");
                            String category = rs.getString("item_category");
                            int count = rs.getInt("item_count");
                            double price = rs.getDouble("item_price");
                            int minStock = rs.getInt("min_stock");
                            double totalValue = count * price;
                            String stockStatus = count <= minStock ? "LOW STOCK" : "OK";
                            
                            if (count <= minStock) lowStockItems++;
                            
                            // Escape commas in name if present
                            if (name.contains(",")) {
                                name = "\"" + name.replace("\"", "\"\"") + "\"";
                            }
                            if (category.contains(",")) {
                                category = "\"" + category.replace("\"", "\"\"") + "\"";
                            }
                            
                            writer.append(String.format("%d,%s,%s,%d,%.2f,%d,%.2f,%s\n", 
                                id, name, category, count, price, minStock, totalValue, stockStatus));
                            
                            grandTotal += totalValue;
                            totalItems++;
                            totalStock += count;
                        }
                        
                        // Add summary at the end
                        writer.append("\n");
                        writer.append("SUMMARY\n");
                        writer.append(String.format("Total Items,%d\n", totalItems));
                        writer.append(String.format("Total Stock Units,%d\n", totalStock));
                        writer.append(String.format("Low Stock Items,%d\n", lowStockItems));
                        writer.append(String.format("Total Inventory Value,%.2f\n", grandTotal));
                        writer.append(String.format("Report Generated,%s\n", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
                        
                        JOptionPane.showMessageDialog(frame, 
                            String.format("Inventory successfully downloaded to:\n%s\n\nSummary:\n• Total Items: %d\n• Total Stock: %d units\n• Low Stock Items: %d\n• Total Value: ₹%.2f", 
                                fileToSave.getAbsolutePath(), totalItems, totalStock, lowStockItems, grandTotal),
                            "Download Complete", 
                            JOptionPane.INFORMATION_MESSAGE);
                            
                    } catch (SQLException | IOException ex) {
                        JOptionPane.showMessageDialog(frame, 
                            "Error downloading inventory: " + ex.getMessage(),
                            "Download Error", 
                            JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, 
                    "Unexpected error: " + ex.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

        static void refreshInventory() {
            SwingUtilities.invokeLater(() -> {
                try (Connection conn = DBConnection.getConnection()) {
                    String q = "SELECT item_id, item_name, item_category, item_count, item_price, COALESCE(min_stock, 5) as min_stock FROM inventory ORDER BY item_id";
                    PreparedStatement ps = conn.prepareStatement(q);
                    ResultSet rs = ps.executeQuery();
                    Vector<String> cols = new Vector<>();
                    cols.add("ID"); cols.add("Name"); cols.add("Category"); cols.add("Count"); cols.add("Price"); cols.add("Min Stock");
                    Vector<Vector<Object>> data = new Vector<>();
                    int lowStockCount = 0;
                    
                    while (rs.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs.getInt("item_id"));
                        row.add(rs.getString("item_name"));
                        row.add(rs.getString("item_category"));
                        int currentStock = rs.getInt("item_count");
                        int minStock = rs.getInt("min_stock");
                        row.add(currentStock);
                        row.add(rs.getDouble("item_price"));
                        row.add(minStock);
                        data.add(row);
                        
                        if (currentStock <= minStock) {
                            lowStockCount++;
                        }
                    }
                    
                    if (inventoryModel == null) inventoryModel = new DefaultTableModel(data, cols);
                    else inventoryModel.setDataVector(data, cols);
                    
                    // Update low stock alert
                    updateLowStockAlert(lowStockCount);
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Failed to load inventory: " + ex.getMessage());
                }
            });
        }
        
        private static void updateLowStockAlert(int lowStockCount) {
            // Find the alert panel in the admin panel
            if (panel != null) {
                Component[] components = panel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JSplitPane) {
                        JSplitPane split = (JSplitPane) comp;
                        Component rightPanel = split.getRightComponent();
                        if (rightPanel instanceof JPanel) {
                            Component[] rightComponents = ((JPanel) rightPanel).getComponents();
                            for (Component rightComp : rightComponents) {
                                if (rightComp instanceof JPanel) {
                                    JPanel alertPanel = (JPanel) rightComp;
                                    if (alertPanel.getBackground().equals(new Color(255, 245, 245))) {
                                        // This is our alert panel
                                        Component[] alertComponents = alertPanel.getComponents();
                                        if (alertComponents.length > 0 && alertComponents[0] instanceof JLabel) {
                                            JLabel alertLabel = (JLabel) alertComponents[0];
                                            if (lowStockCount > 0) {
                                                alertLabel.setText("⚠️ Low Stock Alert: " + lowStockCount + " item(s) need restocking");
                                                alertPanel.setVisible(true);
                                            } else {
                                                alertLabel.setText("✅ All items have adequate stock");
                                                alertPanel.setBackground(new Color(245, 255, 245)); // Light green
                                                alertLabel.setForeground(new Color(0, 120, 0)); // Dark green
                                                alertPanel.setVisible(true);
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    // ---------- CLIENT PANEL (cart-enabled) ----------
    static class ClientPanel {
        private static JPanel panel;
        private static DefaultTableModel inventoryModel;
        private static JTable inventoryTable;

        private static DefaultTableModel cartModel;
        private static JTable cartTable;

        private static JTextField qtyField;
        private static JLabel totalLabel;

        static JPanel getPanel() {
            if (panel == null) panel = createPanel();
            return panel;
        }

        private static JPanel createPanel() {
            panel = new JPanel(new BorderLayout(10,10));
            panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

            JLabel hdr = new JLabel("Client Panel — Shop", SwingConstants.CENTER);
            hdr.setFont(new Font("SansSerif", Font.BOLD, 20));
            panel.add(hdr, BorderLayout.NORTH);

            // center: inventory top, cart bottom
            JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            centerSplit.setResizeWeight(0.6); // Give inventory 60% of space
            centerSplit.setDividerLocation(300);

            // Inventory area
            JPanel inv = new JPanel(new BorderLayout());
            inv.setBorder(BorderFactory.createTitledBorder("Available Inventory"));

            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(new JLabel("Search:"));
            JTextField searchField = new JTextField(18);
            controls.add(searchField);
            controls.add(new JLabel("Qty:"));
            qtyField = new JTextField(4);
            qtyField.setText("1");
            controls.add(qtyField);
            JButton addToCartBtn = new JButton("Add to Cart");
            controls.add(addToCartBtn);
            inv.add(controls, BorderLayout.NORTH);

            // Initialize the inventory model with column names
            inventoryModel = new DefaultTableModel(new String[]{"ID","Name","Category","Count","Price"}, 0);
            inventoryTable = new JTable(inventoryModel) {
                public boolean isCellEditable(int row, int col) { return false; }
            };
            inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            inventoryTable.setRowHeight(24);
            
            // Set up the scroll pane with minimum size
            JScrollPane inventoryScrollPane = new JScrollPane(inventoryTable);
            inventoryScrollPane.setPreferredSize(new Dimension(700, 250));
            inventoryScrollPane.setMinimumSize(new Dimension(500, 200));
            inv.add(inventoryScrollPane, BorderLayout.CENTER);

            // filter
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(inventoryModel);
            inventoryTable.setRowSorter(sorter);
            searchField.getDocument().addDocumentListener(new DocumentListener() {
                private void filter() {
                    String text = searchField.getText();
                    if (text.trim().isEmpty()) sorter.setRowFilter(null);
                    else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
                public void insertUpdate(DocumentEvent e) { filter(); }
                public void removeUpdate(DocumentEvent e) { filter(); }
                public void changedUpdate(DocumentEvent e) { filter(); }
            });

            centerSplit.setTopComponent(inv);

            // Cart area
            JPanel cartPanel = new JPanel(new BorderLayout());
            cartPanel.setBorder(BorderFactory.createTitledBorder("Cart"));

            cartModel = new DefaultTableModel(new String[] {"ID","Name","Qty","Price","Total"}, 0) {
                public Class<?> getColumnClass(int col) {
                    if (col == 0 || col == 2) return Integer.class;
                    if (col == 3 || col == 4) return Double.class;
                    return Object.class;
                }
                public boolean isCellEditable(int row, int col) {
                    return col == 2; // qty editable
                }
            };
            cartTable = new JTable(cartModel);
            cartTable.setRowHeight(24);
            JScrollPane cartScrollPane = new JScrollPane(cartTable);
            cartScrollPane.setPreferredSize(new Dimension(700, 150));
            cartPanel.add(cartScrollPane, BorderLayout.CENTER);

            JPanel cartControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton removeRowBtn = new JButton("Remove Selected");
            JButton clearCartBtn = new JButton("Clear Cart");
            JButton checkoutBtn = new JButton("Checkout");
            totalLabel = new JLabel("Total: ₹0.00");
            cartControls.add(totalLabel);
            cartControls.add(removeRowBtn);
            cartControls.add(clearCartBtn);
            cartControls.add(checkoutBtn);
            cartPanel.add(cartControls, BorderLayout.SOUTH);

            centerSplit.setBottomComponent(cartPanel);

            panel.add(centerSplit, BorderLayout.CENTER);

            // bottom logout
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton logoutBtn = new JButton("Logout");
            bottom.add(logoutBtn);
            panel.add(bottom, BorderLayout.SOUTH);

            // Add to Cart logic
            addToCartBtn.addActionListener(e -> {
                int sel = inventoryTable.getSelectedRow();
                if (sel == -1) { JOptionPane.showMessageDialog(frame, "Select an inventory row first."); return; }
                int modelRow = inventoryTable.convertRowIndexToModel(sel);
                int id = ((Number) inventoryModel.getValueAt(modelRow, 0)).intValue();
                String name = inventoryModel.getValueAt(modelRow, 1).toString();
                int available = ((Number) inventoryModel.getValueAt(modelRow, 3)).intValue();
                double price = ((Number) inventoryModel.getValueAt(modelRow, 4)).doubleValue();

                int qty;
                try { qty = Integer.parseInt(qtyField.getText().trim()); }
                catch (NumberFormatException ex) { JOptionPane.showMessageDialog(frame, "Invalid quantity"); return; }
                if (qty <= 0) { JOptionPane.showMessageDialog(frame, "Quantity must be > 0"); return; }
                if (qty > available) { JOptionPane.showMessageDialog(frame, "Not enough stock. Available: " + available); return; }

                // merge if same item exists in cart
                boolean merged = false;
                for (int r = 0; r < cartModel.getRowCount(); r++) {
                    int cid = ((Number) cartModel.getValueAt(r, 0)).intValue();
                    if (cid == id) {
                        int oldQty = ((Number) cartModel.getValueAt(r, 2)).intValue();
                        int newQty = oldQty + qty;
                        cartModel.setValueAt(newQty, r, 2);
                        cartModel.setValueAt(newQty * price, r, 4);
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    cartModel.addRow(new Object[]{id, name, qty, price, qty * price});
                }
                recalcCartTotal();
            });

            // cart qty edits listener (updates line total)
            cartModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int col = e.getColumn();
                    if (row >= 0 && col == 2) { // qty changed
                        try {
                            int q = ((Number) cartModel.getValueAt(row, 2)).intValue();
                            double pr = ((Number) cartModel.getValueAt(row, 3)).doubleValue();
                            if (q <= 0) {
                                cartModel.removeRow(row);
                            } else {
                                cartModel.setValueAt(q * pr, row, 4);
                            }
                            recalcCartTotal();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame, "Invalid quantity in cart.");
                        }
                    }
                }
            });

            removeRowBtn.addActionListener(e -> {
                int selRow = cartTable.getSelectedRow();
                if (selRow == -1) { JOptionPane.showMessageDialog(frame, "Select cart row to remove."); return; }
                int modelRow = cartTable.convertRowIndexToModel(selRow);
                cartModel.removeRow(modelRow);
                recalcCartTotal();
            });

            clearCartBtn.addActionListener(e -> {
                cartModel.setRowCount(0);
                recalcCartTotal();
            });

            checkoutBtn.addActionListener(e -> {
                if (cartModel.getRowCount() == 0) { JOptionPane.showMessageDialog(frame, "Cart empty."); return; }
                // transactionally deduct
                try (Connection conn = DBConnection.getConnection()) {
                    conn.setAutoCommit(false);
                    boolean ok = true;
                    StringBuilder rec = new StringBuilder();
                    double grand = 0;
                    for (int r = 0; r < cartModel.getRowCount(); r++) {
                        int id = ((Number) cartModel.getValueAt(r, 0)).intValue();
                        String name = cartModel.getValueAt(r, 1).toString();
                        int qty = ((Number) cartModel.getValueAt(r, 2)).intValue();
                        double price = ((Number) cartModel.getValueAt(r, 3)).doubleValue();

                        String check = "SELECT item_count FROM inventory WHERE item_id = ? FOR UPDATE";
                        PreparedStatement psCheck = conn.prepareStatement(check);
                        psCheck.setInt(1, id);
                        ResultSet rs = psCheck.executeQuery();
                        if (rs.next()) {
                            int stock = rs.getInt("item_count");
                            if (stock < qty) {
                                JOptionPane.showMessageDialog(frame, "Not enough stock for " + name + " (available " + stock + ")", "Stock error", JOptionPane.ERROR_MESSAGE);
                                ok = false;
                                break;
                            } else {
                                int newStock = stock - qty;
                                String upd = "UPDATE inventory SET item_count = ? WHERE item_id = ?";
                                PreparedStatement psUpd = conn.prepareStatement(upd);
                                psUpd.setInt(1, newStock);
                                psUpd.setInt(2, id);
                                psUpd.executeUpdate();

                                rec.append(String.format("%s x%d @ %.2f = %.2f%n", name, qty, price, qty * price));
                                grand += qty * price;
                            }
                        } else {
                            JOptionPane.showMessageDialog(frame, "Item missing from DB: " + name, "Error", JOptionPane.ERROR_MESSAGE);
                            ok = false;
                            break;
                        }
                    }

                    if (ok) {
                        conn.commit();
                        // receipt
                        StringBuilder sb = new StringBuilder();
                        sb.append("----- Receipt -----\n");
                        sb.append(rec);
                        sb.append("-------------------\n");
                        sb.append(String.format("Total: ₹ %.2f%n", grand));
                        JTextArea ta = new JTextArea(sb.toString());
                        ta.setEditable(false);
                        JOptionPane.showMessageDialog(frame, new JScrollPane(ta), "Receipt", JOptionPane.INFORMATION_MESSAGE);

                        cartModel.setRowCount(0);
                        recalcCartTotal();
                        refreshInventory();
                    } else {
                        conn.rollback();
                    }
                    conn.setAutoCommit(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Sale failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            logoutBtn.addActionListener(e -> cardLayout.show(rootPanel, "login"));

            // Load inventory data AFTER all UI components are set up
            refreshInventory();

            return panel;
        }

        static void refreshInventory() {
            try (Connection conn = DBConnection.getConnection()) {
                String q = "SELECT item_id, item_name, item_category, item_count, item_price FROM inventory ORDER BY item_id";
                PreparedStatement ps = conn.prepareStatement(q);
                ResultSet rs = ps.executeQuery();
                
                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("item_id"));
                    row.add(rs.getString("item_name"));
                    row.add(rs.getString("item_category"));
                    row.add(rs.getInt("item_count"));
                    row.add(rs.getDouble("item_price"));
                    data.add(row);
                }
                
                // Update on EDT
                SwingUtilities.invokeLater(() -> {
                    if (inventoryModel == null) {
                        return;
                    }
                    // Clear existing data and add new data
                    inventoryModel.setRowCount(0);
                    for (Vector<Object> row : data) {
                        inventoryModel.addRow(row);
                    }
                    inventoryModel.fireTableDataChanged();
                });
                
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Failed to load inventory: " + ex.getMessage());
                });
            }
        }

        static void recalcCartTotal() {
            double total = 0;
            for (int r = 0; r < cartModel.getRowCount(); r++) {
                total += ((Number) cartModel.getValueAt(r, 4)).doubleValue();
            }
            totalLabel.setText(String.format("Total: ₹ %.2f", total));
        }
    }
}