# 📦 Inventory Management System — Admin & Client Panels (Java + PostgreSQL)

A **Java Swing + PostgreSQL** desktop app for managing inventory with **separate Admin and Client interfaces**.  
Built as a self-learning project, it now includes a **point-of-sale (POS)** style flow, with cart handling, stock deduction, and receipts.

---

## 🎯 Objective

This project started as a basic inventory tracker and evolved into a **mini retail management system**.  
The goal was to:
- Learn **Java Swing GUI** development in-depth.
- Understand **multi-user workflows** (Admin vs Client).
- Implement **database transactions** with PostgreSQL.
- Create a realistic POS-like cart & checkout flow.

---

## 🛠️ Tech Stack

| Layer        | Technology         |
|--------------|--------------------|
| Frontend     | Java Swing         |
| Backend      | PostgreSQL         |
| DB Access    | JDBC (PostgreSQL driver) |
| Config       | `.properties` file for secure DB credentials |
| IDE Used     | VS Code / IntelliJ / Terminal |
| OS Tested    | macOS / Windows    |

---

## 🔧 Features

### 🛡️ **Admin Panel**
- ➕ Add new items (category, stock count, price).
- 🖊️ Update existing items directly in the table.
- 🗑️ Remove items or adjust stock levels.
- 📊 View and download the full inventory list with summary.

### 🛍️ **Client Panel**
- 🛒 Browse available stock.
- 🔍 Search/filter items live by name or category.
- 📥 Add items to cart with adjustable quantities.
- ✏️ Update or remove cart items in real-time.
- ✅ Checkout with **"Checkout"** button — deducts stock from DB.
- 🧾 View and print-like a receipt of purchased items.

---

## ⚙️ Setup Instructions

1. **Install PostgreSQL** and create a database:
   ```sql
   CREATE DATABASE inventorymanager;
   ```

2. **Create the table**:
   ```sql
   CREATE TABLE inventory (
       item_id SERIAL PRIMARY KEY,
       item_name VARCHAR(100),
       item_category VARCHAR(100),
       item_count INT,
       item_price NUMERIC(10, 2)
   );
   ```

3. **Create a `config.properties` file**:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/inventorymanager
   db.user=your_username
   db.pass=your_password
   ```

4. **Download PostgreSQL JDBC driver**  
   Place `postgresql-42.7.3.jar` inside a `lib/` folder in your project.

5. **Compile the project**:
   ```bash
   javac -cp ".:lib/*" DBConnection.java InventoryManager.java
   ```

6. **Run the project**:
   ```bash
   java -cp ".:lib/*" InventoryManager
   ```
   > Windows users: replace `:` with `;` in the classpath.

---

## 🗂️ Project Structure

```
Inventory Management/
├── InventoryManager.java       # Main GUI + Admin/Client logic
├── DBConnection.java           # Database connection helper
├── config.properties           # Database credentials
├── lib/
│   └── postgresql-42.7.3.jar   # JDBC driver
└── README.md
```

---

## 🧪 Future Improvements
- 🔐 Separate login for admin and client with DB-stored credentials.
- 🖨️ Print/export receipts.
- 📦 Add stock import/export from CSV.
- 🎨 Enhanced UI with JavaFX or modern themes.

---

> Built with curiosity, caffeine, and a dash of stubbornness 🚀  
> — *Sarthak (Sam)*
