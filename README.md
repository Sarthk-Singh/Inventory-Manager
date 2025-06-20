# 📦 Inventory Management System (Java + PostgreSQL)

This is a self-learning **mini project** built using **Java Swing** for the frontend and **PostgreSQL** as the backend database. It serves as a basic but functional desktop application to manage inventory for small-scale setups — like stores, labs, or internal use.

---

## 🎯 Objective

The goal of this project was to:
- Explore **Java GUI development** with Swing.
- Learn **database integration** using JDBC.
- Understand CRUD operations in a real-world scenario.
- Gain hands-on experience in building structured applications independently.

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

- ➕ **Add Items** — Add new inventory items with category, count, and price.
- 🔁 **Smart Add** — If an item with the same name, category, *and* price exists, it increases its count instead of duplicating it.
- 🔄 **Update Items** — Modify count and price of an existing item.
- ➖ **Remove Items** — Reduce item count (only if enough stock exists).
- 📊 **Show Inventory** — View all items in a scrollable table (`JTable`).
- 🚪 **Exit Button** — Clean exit from the GUI.

---

## ⚙️ Setup Instructions

1. **Install PostgreSQL** and create a database named:
   ```
   InventoryManager
   ```

2. **Create the table** using:
   ```sql
   CREATE TABLE inventory (
       item_name VARCHAR(100),
       item_category VARCHAR(100),
       item_count INT,
       item_price NUMERIC(10, 2)
   );
   ```

3. **Create a `config.properties` file** in the same directory with your database credentials:
   ```
   db.url=jdbc:postgresql://localhost:5432/InventoryManager
   db.user=your_username
   db.pass=your_password
   ```

4. **Download the PostgreSQL JDBC driver** (`postgresql-42.7.3.jar`) and place it in a folder called `lib/` in your project directory.

5. **Compile the project**:
   ```bash
   javac -cp ".:lib/*" DBConnection.java InventoryManager.java
   ```

6. **Run the project**:
   ```bash
   java -cp ".:lib/*" InventoryManager
   ```

   > **Note for Windows users**: Replace `:` with `;` in the classpath:
   ```bat
   javac -cp ".;lib/*" DBConnection.java InventoryManager.java
   java -cp ".;lib/*" InventoryManager
   ```

---

## 🗂️ Project Structure

```
Inventory Management/
├── InventoryManager.java       # Main GUI + logic
├── DBConnection.java           # Reusable DB connector
├── config.properties           # DB config file
├── lib/
│   └── postgresql-42.7.3.jar   # JDBC driver
└── README.md
```

---

## 🤝 Acknowledgements

- This project was not guided by a course or instructor.
- It was built entirely through experimentation, debugging, and iteration.
- ChatGPT was used as a coding assistant during the build.

---

## 🧪 Future Improvements

- Add login/auth system
- Export inventory to CSV
- Sort/filter/search capabilities
- Better UI (themes, layout)

---

> Built with curiosity, caffeine, and Stack Overflow tabs 🧠☕💻  
> — *Sarthak (aka Sam)*  
