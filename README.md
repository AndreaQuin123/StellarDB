# StellarDB
StellarDB is a Java Swing project that works as a **Database Manager** for SQL Anywhere and PostgreSQL.  

It was made for class as a way to practice working with system tables. The program lets you connect to a database, explore its objects, view the DDL, and even generate simple ER diagrams.

It can also migrate schemas from SQL Anywhere to PostgreSQL.

---

## Main Features
- **Connection Manager**  
  Save and switch between multiple database connections.

- **Object Explorer**  
  Browse tables, views, procedures, functions, sequences, triggers, indexes, users, and tablespaces.

- **DDL Viewer**  
  Shows the SQL definition of each object.

- **Schema Migration**  
  Converts SQL Anywhere schema into PostgreSQL (common datatypes and constraints).

- **ER Diagram**  
  Creates a simple automatic diagram showing tables and foreign keys.

- **SQL Editor**  
  Run custom SQL queries inside the program.

---

## How to Run
1. Open the project in NetBeans or any IDE with Swing support.  
2. Add the required JDBC drivers (SQL Anywhere + PostgreSQL).
3. Run the `main` class.

---

## Extra
- Tested with SQL Anywhere 17 and PostgreSQL 15.
- Uses system tables, NOT done with information_schema as per project instructions.
