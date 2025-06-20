import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class InventoryManager extends JFrame implements ActionListener 
{
    JTextField nameField, countField, priceField;
    JComboBox<String> categoryBox;
    JButton addButton, removeButton, showStockButton, exitButton,clearButton,updateButton;

    public InventoryManager() 
    {
        setTitle("Inventory Manager");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(7, 2, 10, 10));

        add(new JLabel("Item Name:"));
        nameField = new JTextField(20);
        add(nameField);

        add(new JLabel("Category:"));
        String[] categories = { "Other", "Food", "Stationary", "Kitchen", "Bathroom" };
        categoryBox = new JComboBox<>(categories);
        add(categoryBox);

        add(new JLabel("Count:"));
        countField = new JTextField(20);
        add(countField);

        add(new JLabel("Price:"));
        priceField = new JTextField(20);
        add(priceField);

        addButton = new JButton("Add");
        addButton.addActionListener(this);
        add(addButton);

        removeButton = new JButton("Remove");
        removeButton.addActionListener(this);
        add(removeButton);
        
        updateButton = new JButton("Update");
        updateButton.addActionListener(this);
        add(updateButton);

        clearButton=new JButton("Clear");
        clearButton.addActionListener(e -> {
        nameField.setText("");
        countField.setText("");
        priceField.setText("");
        categoryBox.setSelectedIndex(0);
        });
        add(clearButton);

        showStockButton = new JButton("Show Stock");
        showStockButton.addActionListener(this);
        add(showStockButton);

        exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        add(exitButton);

        setVisible(true);
    }

    public void actionPerformed(ActionEvent e) 
    {
        Object source = e.getSource();

        if (source == addButton) 
        {
            String name = nameField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            int count;
            double price;
            try 
            {
                count = Integer.parseInt(countField.getText().trim());
                price = Double.parseDouble(priceField.getText().trim());
            }
            catch (NumberFormatException ex) 
            {
                JOptionPane.showMessageDialog(this, "Count and Price must be valid numbers.");
                return;
            }
            if (name.isEmpty()) 
            {
                JOptionPane.showMessageDialog(this, "Item name cannot be empty.");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) 
            {
                // Check if the exact item (name + category + price) exists
                String checkSQL = "SELECT item_count FROM inventory WHERE item_name = ? AND item_category = ? AND item_price = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
                checkStmt.setString(1, name);
                checkStmt.setString(2, category);
                checkStmt.setDouble(3, price);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) 
                {
                    // Same item exists → update count
                    int existingCount = rs.getInt("item_count");
                    int newCount = existingCount + count;

                    String updateSQL = "UPDATE inventory SET item_count = ? WHERE item_name = ? AND item_category = ? AND item_price = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                    updateStmt.setInt(1, newCount);
                    updateStmt.setString(2, name);
                    updateStmt.setString(3, category);
                    updateStmt.setDouble(4, price);
                    updateStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Item exists. Updated count to: " + newCount);
                } 
                else 
                {
                    // New item (same name/category, but different price or totally new) → insert
                    String insertSQL = "INSERT INTO inventory (item_name, item_category, item_count, item_price) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
                    insertStmt.setString(1, name);
                    insertStmt.setString(2, category);
                    insertStmt.setInt(3, count);
                    insertStmt.setDouble(4, price);
                    insertStmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "New item added!");
                }
                // Clear fields
                nameField.setText("");
                countField.setText("");
                priceField.setText("");
                categoryBox.setSelectedIndex(0);

            } 
            catch (SQLException ex) 
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error.");
            }
            catch(Exception ex)
            {
                System.out.println(ex);
            }
        }
        else if (source==removeButton) 
        {
            String name = nameField.getText().trim();
            String category = (String) categoryBox.getSelectedItem();
            int countToRemove;

            try 
            {
                countToRemove = Integer.parseInt(countField.getText().trim());
            } 
            catch (NumberFormatException ex) 
            {
                JOptionPane.showMessageDialog(this, "Invalid count entered.");
                return;
            }
            if (name.isEmpty()) 
            {
                JOptionPane.showMessageDialog(this, "Item name cannot be empty.");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) 
            {
                String selectSQL = "SELECT item_count FROM inventory WHERE item_name = ? AND item_category = ?";
                PreparedStatement selectStmt = conn.prepareStatement(selectSQL);
                selectStmt.setString(1, name);
                selectStmt.setString(2, category);
                ResultSet rs = selectStmt.executeQuery();
                
                if (rs.next())
                {
                    int currentCount = rs.getInt("item_count");

                    if (currentCount >= countToRemove) 
                    {
                        int newCount = currentCount - countToRemove;

                        String updateSQL = "UPDATE inventory SET item_count = ? WHERE item_name = ? AND item_category = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                        updateStmt.setInt(1, newCount);
                        updateStmt.setString(2, name);
                        updateStmt.setString(3, category);

                        int rowsAffected = updateStmt.executeUpdate();
                        if (rowsAffected > 0) 
                        {
                            JOptionPane.showMessageDialog(this, "Stock updated ✅ New count: " + newCount);
                        } 
                        else 
                        {
                            JOptionPane.showMessageDialog(this, "Update failed ❌");
                        }
                    } 
                    else 
                    {
                        JOptionPane.showMessageDialog(this, "Not enough stock to remove ❌");
                    }
                } 
                else 
                {
                    JOptionPane.showMessageDialog(this, "Item not found in database ❌");
                }
            } 
            catch (SQLException ex) 
            {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error occurred ❌");
            }
            catch(Exception ex)
            {
                System.out.println(ex);
            }
        }

        else if(source == showStockButton)
        {
            try (Connection conn = DBConnection.getConnection()) 
            {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");

                // Convert ResultSet to TableModel
                ResultSetMetaData meta = rs.getMetaData();
                int columns = meta.getColumnCount();

                // Get column names
                String[] columnNames = new String[columns];
                for (int i = 1; i <= columns; i++) 
                {
                    columnNames[i - 1] = meta.getColumnName(i);
                }

                // Get row data
                DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
                while (rs.next()) 
                {
                    Object[] row = new Object[columns];
                    for (int i = 1; i <= columns; i++) 
                    {
                        row[i - 1] = rs.getObject(i);
                    }
                    tableModel.addRow(row);
                }

                JTable table = new JTable(tableModel);
                JScrollPane scrollPane = new JScrollPane(table);
                
                // wrap in a separate JFrame or dialog
                JFrame stockFrame = new JFrame("Inventory Stock");
                stockFrame.add(scrollPane);
                stockFrame.setSize(600, 400);
                stockFrame.setLocationRelativeTo(null);
                stockFrame.setVisible(true);
            } 
            catch (Exception ex) 
            {
                JOptionPane.showMessageDialog(this, "Error displaying stock: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        else if (source == updateButton) 
        {
        String name = nameField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();
        int newCount;
        double newPrice;

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item name cannot be empty.");
            return;
        }

        try {
            newCount = Integer.parseInt(countField.getText().trim());
            newPrice = Double.parseDouble(priceField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Count and Price must be valid numbers.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String checkSQL = "SELECT * FROM inventory WHERE item_name = ? AND item_category = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
            checkStmt.setString(1, name);
            checkStmt.setString(2, category);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String updateSQL = "UPDATE inventory SET item_count = ?, item_price = ? WHERE item_name = ? AND item_category = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                updateStmt.setInt(1, newCount);
                updateStmt.setDouble(2, newPrice);
                updateStmt.setString(3, name);
                updateStmt.setString(4, category);

                int rowsUpdated = updateStmt.executeUpdate();
                if (rowsUpdated > 0) {
                    JOptionPane.showMessageDialog(this, "Item updated successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update item.");
                }

            } else {
                JOptionPane.showMessageDialog(this, "Item not found in inventory.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
        }
    }

    }

    public static void main(String[] args) 
    {
        new InventoryManager();
    }
}
