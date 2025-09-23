/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package databasemanager_sqlanywhere_andreaquin;

//Librerias de conexion
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

//gui
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import java.awt.Font;
import java.util.List;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

//Para poder mostrar tipo Y nombre en los nodos del arbol (para visualizacion)
class ObjectTree {

    String name;
    String type; // guarda el tipo de objeto que es

    ObjectTree(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 *
 * @author Andrea Quin
 */
public class ManagerFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ManagerFrame.class.getName());

    private ConnectionManager connectionManager;

    public ManagerFrame(ConnectionManager connectionManager) throws SQLException {
        this.connectionManager = connectionManager;

        initComponents();

        //cambia a no tener una conexion activa
        if (connectionManager.hasActiveConnection()) {
            Connection conn = connectionManager.getActiveConnection();
            DBNameLabel.setText(conn.getCatalog());
            CurrentUserLabel.setText(conn.getMetaData().getUserName());
            loadDatabaseObjects();
        } else {
            DBNameLabel.setText("Not connected");
            CurrentUserLabel.setText("Not connected");
        }

        setTitle("StellarDB - Database Manager");
        setDefaultCloseOperation(ManagerFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        //ICON
        ImageIcon icon = new ImageIcon(ManagerFrame.class.getResource("/resources/Icon.png"));
        this.setIconImage(icon.getImage());

        //detalles del objeto, textarea
        ObjectDetails.setEditable(false);
        ObjectDetails.setFont(new Font("Consolas", Font.PLAIN, 14));
        ObjectDetails.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), " === OBJECT DETAILS === ",
                        javax.swing.border.TitledBorder.CENTER,
                        javax.swing.border.TitledBorder.TOP));

        ObjectDetails.setVisible(false);

        //arbol
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("* * * NO DATABASE CONNECTED * * *");
        JTreeObjects.setModel(new DefaultTreeModel(root));
        loadDatabaseObjects();

        //actionlistener para cambiar automaticamente
        JTreeObjects.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) JTreeObjects.getLastSelectedPathComponent();
            if (selected == null || selected.isRoot()) {
                return;
            }

            Object userObj = selected.getUserObject();
            if (userObj instanceof ObjectTree dbObj) {
                if (!"CATEGORY".equals(dbObj.getType())) {
                    try {
                        showObjectDetails(dbObj.getName(), dbObj.getType());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

    }

    //Muestra el DDL del objeto
    private String getObjectDDL(Connection conn, String objectName, String objectType) throws SQLException {
        StringBuilder sb = new StringBuilder();

        switch (objectType.toUpperCase()) {
            case "TABLE" -> {
                String sql = """
                SELECT c.column_name, d.domain_name AS column_type, c.nulls
                FROM systab t
                JOIN systabcol c ON t.table_id = c.table_id
                JOIN sysdomain d ON c.domain_id = d.domain_id
                WHERE t.table_name = ?
                ORDER BY c.column_id
            """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder ddl = new StringBuilder();
                        ddl.append("CREATE TABLE ").append(objectName).append(" (\n");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) {
                                ddl.append(",\n");
                            }
                            ddl.append("  ")
                                    .append(rs.getString("column_name"))
                                    .append(" ")
                                    .append(rs.getString("column_type"));
                            if ("N".equals(rs.getString("nulls"))) {
                                ddl.append(" NOT NULL");
                            }
                            first = false;
                        }
                        ddl.append("\n);");

                        if (first) {
                            sb.append("❌ Table not found.");
                        } else {
                            sb.append(ddl.toString());
                        }
                    }
                }
            }
            case "VIEW" -> {
                String sql = """
                SELECT t.table_name AS view_name, u.user_name AS schema_name, v.view_def
                FROM sysview v
                JOIN systab t ON v.view_object_id = t.object_id
                JOIN sysuser u ON t.creator = u.user_id
                WHERE t.table_name = ?
            """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String schema = rs.getString("schema_name");
                            String viewDef = rs.getString("view_def");

                            viewDef = viewDef.replaceAll("(?i)^CREATE\\s+VIEW\\s+\"?[\\w]+\"?\\.\"?[\\w]+\"?\\s+AS\\s+", "");

                            sb.append("CREATE VIEW \"").append(schema).append("\".\"")
                                    .append(objectName).append("\" AS\n")
                                    .append(viewDef).append(";");
                        } else {
                            sb.append("❌ View not found.");
                        }
                    }
                }
            }
            case "SEQUENCE" -> {
                String sql = """
                SELECT sequence_name, start_with, increment_by, min_value, max_value, cycle 
                FROM syssequence 
                WHERE sequence_name = ?
            """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sb.append("CREATE SEQUENCE ").append(objectName).append("\n")
                                    .append("START WITH ").append(rs.getLong("start_with")).append("\n")
                                    .append("INCREMENT BY ").append(rs.getLong("increment_by")).append("\n")
                                    .append("MINVALUE ").append(rs.getLong("min_value")).append("\n")
                                    .append("MAXVALUE ").append(rs.getLong("max_value")).append("\n")
                                    .append(rs.getInt("cycle") == 1 ? "CYCLE;" : "NO CYCLE;");
                        } else {
                            sb.append("❌ Sequence not found.");
                        }
                    }
                }
            }
            case "PROCEDURE" -> {
                String sql = "SELECT proc_name, proc_defn FROM sysprocedure WHERE proc_name = ?";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sb.append("CREATE PROCEDURE ").append(rs.getString("proc_name")).append(" AS\n");
                            sb.append(rs.getString("proc_defn")).append(";");
                        } else {
                            sb.append("❌ Procedure not found.");
                        }
                    }
                }
            }
            case "TRIGGER" -> {
                String sql = """
        SELECT t.trigger_name, s.table_name, t.event, t.trigger_time, t.trigger_defn, t.source
        FROM systrigger t
        JOIN systab s ON t.table_id = s.table_id
        WHERE t.trigger_name = ?
    """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String triggerTimeCode = rs.getString("trigger_time");
                            String eventCode = rs.getString("event");
                            String tableName = rs.getString("table_name");
                            String triggerBody = rs.getString("source");

                            String triggerTime = switch (triggerTimeCode) {
                                case "A" ->
                                    "AFTER";
                                case "B" ->
                                    "BEFORE";
                                case "I" ->
                                    "INSTEAD OF";
                                case "K" ->
                                    "INSTEAD OF (statement-level)";
                                case "R" ->
                                    "RESOLVE";
                                case "S" ->
                                    "AFTER (statement-level)";
                                default ->
                                    triggerTimeCode;
                            };

                            String event = switch (eventCode) {
                                case "A" ->
                                    "INSERT OR DELETE";
                                case "B" ->
                                    "INSERT OR UPDATE";
                                case "C" ->
                                    "UPDATE COLUMNS";
                                case "D" ->
                                    "DELETE";
                                case "E" ->
                                    "DELETE OR UPDATE";
                                case "I" ->
                                    "INSERT";
                                case "U" ->
                                    "UPDATE";
                                case "M" ->
                                    "INSERT, DELETE, UPDATE";
                                default ->
                                    eventCode;
                            };

                            sb.append("CREATE TRIGGER ").append(rs.getString("trigger_name")).append("\n")
                                    .append(triggerTime).append(" ").append(event)
                                    .append(" ON ").append(tableName).append("\n")
                                    .append("AS\n")
                                    .append(triggerBody).append(";");
                        } else {
                            sb.append("❌ Trigger not found.");
                        }
                    }
                }
            }

            case "INDEX" -> {
                String sql = """
                    SELECT i.index_name, t.table_name,
                           c.column_name, ic.column_id AS column_position
                    FROM sysidx i
                    JOIN systab t ON i.table_id = t.table_id
                    JOIN sysidxcol ic ON i.index_id = ic.index_id AND i.table_id = ic.table_id
                    JOIN systabcol c ON ic.table_id = c.table_id AND ic.column_id = c.column_id
                    WHERE i.index_name = ?
                    ORDER BY ic.column_id
                """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, objectName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String tableName = rs.getString("table_name");

                            StringBuilder cols = new StringBuilder();
                            do {
                                if (cols.length() > 0) {
                                    cols.append(", ");
                                }
                                cols.append(rs.getString("column_name"));
                            } while (rs.next());

                            sb.append("CREATE ")
                                    .append("INDEX ")
                                    .append(objectName)
                                    .append(" ON ")
                                    .append(tableName)
                                    .append(" (")
                                    .append(cols)
                                    .append(");");
                        } else {
                            sb.append("❌ Index not found.");
                        }
                    }
                }
            }

            default ->
                sb.append("❌ DDL not supported for object type: ").append(objectType);
        }

        return sb.toString();
    }

    private void loadDatabaseObjects() {

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Database Objects");

        DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode(new ObjectTree("Tables", "CATEGORY"));
        DefaultMutableTreeNode viewsNode = new DefaultMutableTreeNode(new ObjectTree("Views", "CATEGORY"));
        DefaultMutableTreeNode proceduresNode = new DefaultMutableTreeNode(new ObjectTree("Procedures", "CATEGORY"));
        DefaultMutableTreeNode sequencesNode = new DefaultMutableTreeNode(new ObjectTree("Sequences", "CATEGORY"));
        DefaultMutableTreeNode triggersNode = new DefaultMutableTreeNode(new ObjectTree("Triggers", "CATEGORY"));
        DefaultMutableTreeNode indexesNode = new DefaultMutableTreeNode(new ObjectTree("Indexes", "CATEGORY"));
        DefaultMutableTreeNode functionsNode = new DefaultMutableTreeNode(new ObjectTree("Functions", "CATEGORY"));
        DefaultMutableTreeNode usersNode = new DefaultMutableTreeNode(new ObjectTree("Users", "CATEGORY"));
        DefaultMutableTreeNode tablespacesNode = new DefaultMutableTreeNode(new ObjectTree("Tablespaces", "CATEGORY"));

        Connection conn = connectionManager.getActiveConnection();

        if (conn == null) {
            return;
        }

        try (Statement st = conn.createStatement()) {

            // === Tables ===
            ResultSet rs = st.executeQuery("""
            SELECT t.table_name, u.user_name AS owner
            FROM systab t
            JOIN sysuser u ON t.creator = u.user_id
            WHERE t.table_type = 1
                AND u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
                AND t.table_name NOT LIKE 'rs_%'
            ORDER BY u.user_name, t.table_name
        """);

            while (rs.next()) {
                String tableName = rs.getString("table_name");
                tablesNode.add(new DefaultMutableTreeNode(new ObjectTree(tableName, "TABLE")));
            }

            // === Views ===
            rs = st.executeQuery("""
            SELECT s.table_name, u.user_name AS owner
            FROM systab s
            JOIN sysview v ON s.object_id = v.view_object_id
            JOIN sysuser u ON s.creator = u.user_id
            WHERE s.table_type = 21
              AND u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
              AND s.table_name NOT LIKE 'rs_%'
            ORDER BY u.user_name, s.table_name
        """);

            while (rs.next()) {
                String viewName = rs.getString("table_name");
                viewsNode.add(new DefaultMutableTreeNode(new ObjectTree(viewName, "VIEW")));
            }

            // === Procedures ===
            rs = st.executeQuery("""
SELECT p.proc_name, u.user_name AS owner
            FROM sysprocedure p
            JOIN sysuser u ON p.creator = u.user_id
            WHERE u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
              AND p.proc_name NOT LIKE 'rs_%'
            ORDER BY u.user_name, p.proc_name
        """);

            while (rs.next()) {
                String procName = rs.getString("proc_name");
                proceduresNode.add(new DefaultMutableTreeNode(new ObjectTree(procName, "PROCEDURE")));
            }

            // === Sequences ===
            rs = st.executeQuery("""
SELECT s.sequence_name, u.user_name AS owner_name
            FROM syssequence s
            JOIN sysuser u ON s.owner = u.user_id
            WHERE u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
              AND s.sequence_name NOT LIKE 'rs_%'
            ORDER BY u.user_name, s.sequence_name
        """);

            while (rs.next()) {
                String seqName = rs.getString("sequence_name");
                sequencesNode.add(new DefaultMutableTreeNode(new ObjectTree(seqName, "SEQUENCE")));
            }

            // === Triggers ===
            rs = st.executeQuery("""
            SELECT trigger_name
            FROM systrigger
            WHERE trigger_name NOT LIKE 'rs_%'
              AND trigger_name NOT LIKE 'SYS%'
            ORDER BY trigger_name
        """);

            while (rs.next()) {
                String trigName = rs.getString("trigger_name");
                triggersNode.add(new DefaultMutableTreeNode(new ObjectTree(trigName, "TRIGGER")));
            }

            // === Indexes ===
            rs = st.executeQuery("""
            SELECT i.index_name, t.table_name
            FROM sysidx i
            JOIN systab t ON i.table_id = t.table_id
            JOIN sysuser u ON t.creator = u.user_id
            WHERE u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
              AND t.table_type = 1       -- only user tables
              AND i.index_name NOT LIKE 'rs_%'
              AND i.index_name NOT LIKE 'SYS_%'
            ORDER BY t.table_name, i.index_name
        """);
            while (rs.next()) {
                String indexName = rs.getString("index_name");
                indexesNode.add(new DefaultMutableTreeNode(new ObjectTree(indexName, "INDEX")));
            }

            // === Functions ===
            rs = st.executeQuery("""
            SELECT p.proc_name, u.user_name AS owner
            FROM sysprocedure p
            JOIN sysuser u ON p.creator = u.user_id
            WHERE u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
              AND p.proc_name NOT LIKE 'rs_%'
              AND p.proc_defn LIKE 'CREATE FUNCTION%' -- Only functions
            ORDER BY u.user_name, p.proc_name
        """);

            while (rs.next()) {
                String funcName = rs.getString("proc_name");
                functionsNode.add(new DefaultMutableTreeNode(new ObjectTree(funcName, "FUNCTION")));
            }

            // === Users ===
            rs = st.executeQuery("""
            SELECT user_name
                        FROM sysuser
                        WHERE user_name NOT LIKE 'SYS%'
                          AND user_name NOT LIKE 'sa%'
                          AND user_name NOT LIKE 'dbo'
                          AND user_name NOT LIKE 'rs_%' 
                          AND user_name NOT LIKE 'extenv%' 
                          AND user_name NOT LIKE 'diagnostics' 
                          AND user_name NOT LIKE 'COCKPIT_%'
                          AND user_name NOT LIKE 'PUBLIC'
                        ORDER BY user_name;
        """);

            while (rs.next()) {
                String userName = rs.getString("user_name");
                usersNode.add(new DefaultMutableTreeNode(new ObjectTree(userName, "USER")));
            }

            rs = st.executeQuery("SELECT dbspace_name FROM sysdbspace");

            while (rs.next()) {
                String dbspaceName = rs.getString("dbspace_name");
                tablespacesNode.add(new DefaultMutableTreeNode(new ObjectTree(dbspaceName, "TABLESPACE")));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading objects: " + ex.getMessage());
            ex.printStackTrace();
        }

        root.add(tablesNode);
        root.add(viewsNode);
        root.add(proceduresNode);
        root.add(sequencesNode);
        root.add(triggersNode);
        root.add(indexesNode);
        root.add(functionsNode);
        root.add(usersNode);
        root.add(tablespacesNode);

        JTreeObjects.setModel(new DefaultTreeModel(root));
        JTreeObjects.expandRow(0);
    }

    private String showTableData(String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Table: ").append(tableName).append("\n\n");

        Connection conn = connectionManager.getActiveConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "No active connection!");
            return "❌ No active connection!";
        }

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                sb.append(String.format("%-20s", meta.getColumnName(i)));
            }
            sb.append("\n").append("-".repeat(columnCount * 20)).append("\n");

            int rowCount = 0;
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    sb.append(String.format("%-20s", rs.getString(i)));
                }
                sb.append("\n");
                rowCount++;
            }

            if (rowCount == 0) {
                sb.append("❌ No rows found in table.\n");
            }

        } catch (SQLException e) {
            sb.append("❌ Error retrieving table: ").append(e.getMessage()).append("\n");
        }

        return sb.toString();
    }

    private String showTablespaces(Connection conn) {
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Tablespaces\n\n");

        String sql = "SELECT dbspace_id, dbspace_name, object_id FROM sysdbspace";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            // Header
            sb.append(String.format("%-10s%-30s%-15s\n", "ID", "Name", "Object ID"));
            sb.append("-".repeat(55)).append("\n");

            int count = 0;
            while (rs.next()) {
                sb.append(String.format("%-10d%-30s%-15d\n",
                        rs.getInt("dbspace_id"),
                        rs.getString("dbspace_name").trim(),
                        rs.getLong("object_id")
                ));
                count++;
            }

            if (count == 0) {
                sb.append("❌ No tablespaces found.\n");
            }

        } catch (SQLException ex) {
            sb.append("❌ Error loading tablespaces: ").append(ex.getMessage()).append("\n");
        }

        return sb.toString();
    }

    private void showObjectDetails(String objectName, String objectType) throws SQLException {
        ObjectDetails.setText("");
        ObjectDetails.setVisible(false);

        Connection conn = connectionManager.getActiveConnection();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "No active connection!");
            ObjectDetails.setText("❌ No active connection!");
            ObjectDetails.setVisible(true);
            return;
        }

        switch (objectType.toUpperCase()) {
            case "TABLE" -> {
                ObjectDetails.setText(showTableData(objectName));
                ObjectDetails.setVisible(true);
            }
            case "VIEW" -> {
                ObjectDetails.setText(getViewDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "SEQUENCE" -> {
                ObjectDetails.setText(getSequenceDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "TRIGGER" -> {
                ObjectDetails.setText(getTriggerDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "PROCEDURE" -> {
                ObjectDetails.setText(getProcedureDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "INDEX" -> {
                ObjectDetails.setText(getIndexDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "FUNCTION" -> {
                ObjectDetails.setText(getFunctionDetails(conn, objectName));
                ObjectDetails.setVisible(true);
            }
            case "TABLESPACE" -> {
                ObjectDetails.setText(showTablespaces(conn));
                ObjectDetails.setVisible(true);
            }
            default -> {
                ObjectDetails.setText("No details available for " + objectType);
                ObjectDetails.setVisible(true);
            }
        }

        ObjectDetails.revalidate();
        ObjectDetails.repaint();
    }

    // =============== MUESTRA DETALLES DE LOS OBJETOS
    private String getIndexDetails(Connection conn, String tableName) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String sql
                = "SELECT i.index_name, i.\"unique\", i.index_category, "
                + "       c.sequence, col.column_name, c.\"order\" "
                + "FROM sysidx i "
                + "JOIN sysidxcol c ON i.table_id = c.table_id AND i.index_id = c.index_id "
                + "JOIN syscolumn col ON c.table_id = col.table_id AND c.column_id = col.column_id "
                + "WHERE i.table_id = (SELECT table_id FROM systab WHERE table_name = ?) "
                + "ORDER BY i.index_name, c.sequence";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                String currentIndex = "";
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    if (!indexName.equals(currentIndex)) {
                        if (!currentIndex.isEmpty()) {
                            sb.append("\n");
                        }
                        sb.append("Index: ").append(indexName).append("\n");
                        sb.append("Unique: ").append(rs.getInt("unique") == 1 ? "Yes" : "No").append("\n");
                        sb.append("Category: ").append(rs.getInt("index_category")).append("\n");
                        sb.append("Columns:\n");
                        currentIndex = indexName;
                    }
                    sb.append("  - ").append(rs.getString("column_name"))
                            .append(" (Order: ").append(rs.getString("order")).append(")\n");
                }
            }
        }

        return sb.toString();
    }

    private String getFunctionDetails(Connection conn, String functionName) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT proc_name, proc_defn, remarks, source FROM sysprocedure WHERE proc_name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, functionName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Name: ").append(rs.getString("proc_name")).append("\n");
                    sb.append("Definition:\n").append(rs.getString("proc_defn")).append("\n\n");
                    sb.append("Remarks:\n").append(rs.getString("remarks")).append("\n");
                    sb.append("Source:\n").append(rs.getString("source")).append("\n");
                } else {
                    sb.append("❌ Function not found.");
                }
            }
        }

        return sb.toString();
    }

    private String getViewDetails(Connection conn, String viewName) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql
                = "SELECT v.view_def "
                + "FROM sysview v "
                + "JOIN systab t ON v.view_object_id = t.object_id "
                + "WHERE t.table_name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, viewName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Definition:\n").append(rs.getString("view_def")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String getSequenceDetails(Connection conn, String sequenceName) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql
                = "SELECT sequence_name, start_with, increment_by, min_value, max_value, cycle "
                + "FROM syssequence "
                + "WHERE sequence_name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Start: ").append(rs.getLong("start_with")).append("\n");
                    sb.append("Increment: ").append(rs.getLong("increment_by")).append("\n");
                    sb.append("Min: ").append(rs.getLong("min_value")).append("\n");
                    sb.append("Max: ").append(rs.getLong("max_value")).append("\n");
                    sb.append("Cycle: ").append(rs.getInt("cycle") == 1 ? "YES" : "NO").append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String getProcedureDetails(Connection conn, String procedureName) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql
                = "SELECT proc_name, proc_defn "
                + "FROM sysprocedure "
                + "WHERE proc_name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, procedureName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Name: ").append(rs.getString("proc_name")).append("\n");
                    sb.append("Definition:\n").append(rs.getString("proc_defn")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String getTriggerDetails(Connection conn, String triggerName) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT t.trigger_name, s.table_name, t.event, t.trigger_time, t.source "
                + "FROM systrigger t "
                + "JOIN systab s ON t.table_id = s.table_id "
                + "WHERE t.trigger_name = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, triggerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append("Trigger on table: ").append(rs.getString("table_name")).append("\n");

                    String eventCode = rs.getString("event");
                    String eventDesc = switch (eventCode) {
                        case "A" ->
                            "INSERT, DELETE";
                        case "B" ->
                            "INSERT, UPDATE";
                        case "C" ->
                            "UPDATE COLUMNS";
                        case "D" ->
                            "DELETE";
                        case "E" ->
                            "DELETE, UPDATE";
                        case "I" ->
                            "INSERT";
                        case "U" ->
                            "UPDATE";
                        case "M" ->
                            "INSERT, DELETE, UPDATE";
                        default ->
                            eventCode;
                    };
                    sb.append("Event: ").append(eventDesc).append("\n");

                    String timeCode = rs.getString("trigger_time");
                    String timeDesc = switch (timeCode) {
                        case "A" ->
                            "AFTER (row-level)";
                        case "B" ->
                            "BEFORE (row-level)";
                        case "I" ->
                            "INSTEAD OF (row-level)";
                        case "K" ->
                            "INSTEAD OF (statement-level)";
                        case "R" ->
                            "RESOLVE";
                        case "S" ->
                            "AFTER (statement-level)";
                        default ->
                            timeCode;
                    };
                    sb.append("Trigger Time: ").append(timeDesc).append("\n");

                    sb.append("\nDefinition:\n").append(rs.getString("source")).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        JTreeObjects = new javax.swing.JTree();
        jScrollPane2 = new javax.swing.JScrollPane();
        ObjectDetails = new javax.swing.JTextArea();
        SQLEditorButton = new javax.swing.JButton();
        CreateObjectButton = new javax.swing.JButton();
        ShowDDLButton = new javax.swing.JButton();
        SincronizacionButton = new javax.swing.JButton();
        DisconnectButton = new javax.swing.JButton();
        ChangeConnectionButton = new javax.swing.JButton();
        ConnectionLabel1 = new javax.swing.JLabel();
        ConnectionLabel = new javax.swing.JLabel();
        DBNameLabel = new javax.swing.JLabel();
        CurrentUserLabel = new javax.swing.JLabel();
        Background = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(183, 197, 211));

        jPanel1.setBackground(new java.awt.Color(166, 190, 213));
        jPanel1.setLayout(null);

        jScrollPane1.setBackground(new java.awt.Color(0, 0, 0));
        jScrollPane1.setForeground(new java.awt.Color(0, 0, 0));

        JTreeObjects.setBackground(new java.awt.Color(255, 255, 255));
        JTreeObjects.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        JTreeObjects.setFont(new java.awt.Font("Gadugi", 1, 14)); // NOI18N
        JTreeObjects.setForeground(new java.awt.Color(0, 0, 0));
        JTreeObjects.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                JTreeObjectsValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(JTreeObjects);

        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(40, 30, 450, 620);

        ObjectDetails.setEditable(false);
        ObjectDetails.setBackground(new java.awt.Color(255, 255, 255));
        ObjectDetails.setColumns(20);
        ObjectDetails.setForeground(new java.awt.Color(0, 0, 0));
        ObjectDetails.setRows(5);
        ObjectDetails.setBorder(null);
        jScrollPane2.setViewportView(ObjectDetails);

        jPanel1.add(jScrollPane2);
        jScrollPane2.setBounds(530, 30, 620, 340);

        SQLEditorButton.setBorderPainted(false);
        SQLEditorButton.setContentAreaFilled(false);
        SQLEditorButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        SQLEditorButton.setDefaultCapable(false);
        SQLEditorButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SQLEditorButtonMouseClicked(evt);
            }
        });
        SQLEditorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SQLEditorButtonActionPerformed(evt);
            }
        });
        jPanel1.add(SQLEditorButton);
        SQLEditorButton.setBounds(550, 550, 120, 90);

        CreateObjectButton.setBorderPainted(false);
        CreateObjectButton.setContentAreaFilled(false);
        CreateObjectButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        CreateObjectButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                CreateObjectButtonMouseClicked(evt);
            }
        });
        CreateObjectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreateObjectButtonActionPerformed(evt);
            }
        });
        jPanel1.add(CreateObjectButton);
        CreateObjectButton.setBounds(550, 450, 120, 90);

        ShowDDLButton.setBorderPainted(false);
        ShowDDLButton.setContentAreaFilled(false);
        ShowDDLButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ShowDDLButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ShowDDLButtonMouseClicked(evt);
            }
        });
        jPanel1.add(ShowDDLButton);
        ShowDDLButton.setBounds(680, 450, 120, 90);

        SincronizacionButton.setBorderPainted(false);
        SincronizacionButton.setContentAreaFilled(false);
        SincronizacionButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        SincronizacionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SincronizacionButtonMouseClicked(evt);
            }
        });
        SincronizacionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SincronizacionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(SincronizacionButton);
        SincronizacionButton.setBounds(690, 550, 110, 80);

        DisconnectButton.setAutoscrolls(true);
        DisconnectButton.setBorderPainted(false);
        DisconnectButton.setContentAreaFilled(false);
        DisconnectButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        DisconnectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DisconnectButtonActionPerformed(evt);
            }
        });
        jPanel1.add(DisconnectButton);
        DisconnectButton.setBounds(810, 550, 130, 90);

        ChangeConnectionButton.setBorderPainted(false);
        ChangeConnectionButton.setContentAreaFilled(false);
        ChangeConnectionButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ChangeConnectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ChangeConnectionButtonActionPerformed(evt);
            }
        });
        jPanel1.add(ChangeConnectionButton);
        ChangeConnectionButton.setBounds(810, 440, 130, 100);

        ConnectionLabel1.setBackground(new java.awt.Color(255, 255, 255));
        ConnectionLabel1.setForeground(new java.awt.Color(0, 0, 0));
        ConnectionLabel1.setText("SINCRONIZACION");
        ConnectionLabel1.setOpaque(true);
        jPanel1.add(ConnectionLabel1);
        ConnectionLabel1.setBounds(690, 620, 120, 16);

        ConnectionLabel.setBackground(new java.awt.Color(255, 255, 255));
        ConnectionLabel.setForeground(new java.awt.Color(0, 0, 0));
        ConnectionLabel.setText("   CONNECT / CHANGE");
        ConnectionLabel.setOpaque(true);
        jPanel1.add(ConnectionLabel);
        ConnectionLabel.setBounds(800, 520, 150, 16);

        DBNameLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        DBNameLabel.setForeground(new java.awt.Color(0, 0, 0));
        DBNameLabel.setText("N/A");
        jPanel1.add(DBNameLabel);
        DBNameLabel.setBounds(1010, 570, 110, 50);

        CurrentUserLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        CurrentUserLabel.setForeground(new java.awt.Color(0, 0, 0));
        CurrentUserLabel.setText("N/A");
        jPanel1.add(CurrentUserLabel);
        CurrentUserLabel.setBounds(1010, 470, 120, 50);

        Background.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/ManagerFrame.png"))); // NOI18N
        jPanel1.add(Background);
        Background.setBounds(0, 0, 1200, 670);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SQLEditorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SQLEditorButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SQLEditorButtonActionPerformed

    private void CreateObjectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CreateObjectButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_CreateObjectButtonActionPerformed

    private void SincronizacionButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SincronizacionButtonMouseClicked
    }//GEN-LAST:event_SincronizacionButtonMouseClicked


    private void ChangeConnectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ChangeConnectionButtonActionPerformed
        String[] options = {"Use existing connection", "Add new connection"};
        int choice = JOptionPane.showOptionDialog(this,
                "Do you want to use an existing connection or add a new one?",
                "Change Connection",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            
            String[] keys = connectionManager.getConnectionKeys();
            if (keys.length == 0) {
                JOptionPane.showMessageDialog(this, "No existing connections available. Please add a new one.");
                return;
            }
            List<String> sqlanywhereKeys = new ArrayList<>();
            
            for (String key : keys) {
                ConnectionManager.ConnectionInfo info = connectionManager.getSavedConnectionInfo(key);
                if (info.type == true) { // true = SQLAnywhere
                    sqlanywhereKeys.add(key);
                }
            }

            if (sqlanywhereKeys.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No SQLAnywhere connections available.");
                return;
            }

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a SQLAnywhere connection:",
                    "Connections",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    sqlanywhereKeys.toArray(new String[0]),
                    sqlanywhereKeys.get(0));

            if (selected != null) {
                try {
                    connectionManager.setActiveConnection(selected);
                } catch (SQLException ex) {
                    System.getLogger(ManagerFrame.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }

                // Update labels at the bottom
                Connection conn = connectionManager.getActiveConnection();
                try {
                    DBNameLabel.setText(conn.getCatalog());  // database name
                    CurrentUserLabel.setText(conn.getMetaData().getUserName()); // current user

                    loadDatabaseObjects();
                    JTreeObjects.revalidate();
                    JTreeObjects.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (choice == 1) {

            JDialog dialog = new JDialog(this, "Add New Connection", true);
            dialog.setSize(350, 250);
            dialog.setLocationRelativeTo(null);
            dialog.setLayout(new java.awt.GridLayout(5, 2, 5, 5));

            JLabel portLabel = new JLabel("Port:");
            JTextField portField = new JTextField();

            JLabel dbLabel = new JLabel("Database:");
            JTextField dbField = new JTextField();

            JLabel userLabel = new JLabel("User:");
            JTextField userField = new JTextField();

            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();

            JButton okButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            dialog.add(portLabel);
            dialog.add(portField);
            dialog.add(dbLabel);
            dialog.add(dbField);
            dialog.add(userLabel);
            dialog.add(userField);
            dialog.add(passLabel);
            dialog.add(passField);
            dialog.add(okButton);
            dialog.add(cancelButton);

            okButton.addActionListener(ev -> {
                try {
                    int port = Integer.parseInt(portField.getText().trim());
                    String db = dbField.getText().trim();
                    String user = userField.getText().trim();
                    String pass = new String(passField.getPassword());

                    String key = db + "_" + user;
                    ConnectionManager.ConnectionInfo info = new ConnectionManager.ConnectionInfo(port, db, user, pass);

                    connectionManager.addConnection(key, info);

                    Connection conn = connectionManager.getActiveConnection();
                    if (conn != null) {
                        DBNameLabel.setText(conn.getCatalog());
                        CurrentUserLabel.setText(conn.getMetaData().getUserName());

                        loadDatabaseObjects();
                        JTreeObjects.revalidate();
                        JTreeObjects.repaint();
                    }

                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                }
            });

            cancelButton.addActionListener(ev -> dialog.dispose());

            dialog.setVisible(true);

        }

    }//GEN-LAST:event_ChangeConnectionButtonActionPerformed

    private void SQLEditorButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SQLEditorButtonMouseClicked
        SQLEditorFrame dialog = new SQLEditorFrame(connectionManager, this);
        dialog.setVisible(true);
        dialog.setLocationRelativeTo(null);
        this.setVisible(false);
    }//GEN-LAST:event_SQLEditorButtonMouseClicked

    private void JTreeObjectsValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_JTreeObjectsValueChanged
    }//GEN-LAST:event_JTreeObjectsValueChanged

    private void SincronizacionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SincronizacionButtonActionPerformed
        //TO-DO
        //SINCRONIZACION

        String[] options = {"Use existing connection", "Add new connection"};
        int choice = JOptionPane.showOptionDialog(this,
                "Do you want to use an existing connection or add a new one?",
                "Change Connection",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            String[] keys = connectionManager.getConnectionKeys();
            List<String> postgresKeys = new ArrayList<>();
            for (String key : keys) {
                ConnectionManager.ConnectionInfo info = connectionManager.getSavedConnectionInfo(key);
                if (info != null && !info.type) { // false = Postgres
                    postgresKeys.add(key);
                }
            }

            if (postgresKeys.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No Postgres connections available. Please add a new one.");
                return;
            }

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select a Postgres connection:",
                    "Connections",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    postgresKeys.toArray(new String[0]),
                    postgresKeys.get(0)
            );

            if (selected != null) {
                //sincronizar
            }

        } else if (choice == 1) {
            JDialog dialog = new JDialog(this, "Add New Connection to Postgres", true);
            dialog.setSize(350, 250);
            dialog.setLocationRelativeTo(null);
            dialog.setLayout(new java.awt.GridLayout(5, 2, 5, 5));

            JLabel portLabel = new JLabel("Port:");
            JTextField portField = new JTextField();

            JLabel dbLabel = new JLabel("Database:");
            JTextField dbField = new JTextField();

            JLabel userLabel = new JLabel("User:");
            JTextField userField = new JTextField();

            JLabel passLabel = new JLabel("Password:");
            JPasswordField passField = new JPasswordField();

            JButton okButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");

            dialog.add(portLabel);
            dialog.add(portField);
            dialog.add(dbLabel);
            dialog.add(dbField);
            dialog.add(userLabel);
            dialog.add(userField);
            dialog.add(passLabel);
            dialog.add(passField);
            dialog.add(okButton);
            dialog.add(cancelButton);

            okButton.addActionListener(ev -> {
                try {
                    int port = Integer.parseInt(portField.getText().trim());
                    String db = dbField.getText().trim();
                    String user = userField.getText().trim();
                    String pass = new String(passField.getPassword());

                    String key = db + "_" + user;

                    ConnectionManager.ConnectionInfo info = new ConnectionManager.ConnectionInfo(port, db, user, pass);
                    info.type = false; // false = Postgres

                    connectionManager.addConnection(key, info);

                    JOptionPane.showMessageDialog(dialog, "Postgres connection added successfully!");
                    dialog.dispose();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                }
            });

            cancelButton.addActionListener(ev -> dialog.dispose());

            dialog.setVisible(true);
        }
    }//GEN-LAST:event_SincronizacionButtonActionPerformed

    private void DisconnectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DisconnectButtonActionPerformed
        if (!connectionManager.hasActiveConnection()) {
            JOptionPane.showMessageDialog(this, "⚠️ No active connection to disconnect.");
            return;
        }

        try {
            Connection conn = connectionManager.getActiveConnection();
            String user = conn.getMetaData().getUserName();
            String db = conn.getCatalog();

            connectionManager.disconnectActiveConnection();

            DBNameLabel.setText("Not connected");
            CurrentUserLabel.setText("Not connected");

            DefaultTreeModel model = (DefaultTreeModel) JTreeObjects.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            root.removeAllChildren();
            model.reload();

            JOptionPane.showMessageDialog(this, "✅ Disconnected from " + db + " as " + user);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error disconnecting: " + e.getMessage());
            e.printStackTrace();
        }

     }//GEN-LAST:event_DisconnectButtonActionPerformed

    private void ShowDDLButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ShowDDLButtonMouseClicked
        ObjectDetails.setText("");

        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) JTreeObjects.getLastSelectedPathComponent();
        if (selected == null || selected.isRoot()) {
            return;
        }

        Object obj = selected.getUserObject();
        if (obj instanceof ObjectTree dbObj && !"CATEGORY".equals(dbObj.getType())) {
            try {
                String ddl = getObjectDDL(connectionManager.getActiveConnection(), dbObj.getName(), dbObj.getType());
                ObjectDetails.setText(ddl);
                ObjectDetails.setVisible(true);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "❌ Error fetching DDL: " + e.getMessage());
            }
        }    }//GEN-LAST:event_ShowDDLButtonMouseClicked

    private void CreateObjectButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_CreateObjectButtonMouseClicked
        this.dispose();
        CreateObject create = new CreateObject(connectionManager, this);
        create.setVisible(true);
    }//GEN-LAST:event_CreateObjectButtonMouseClicked

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Background;
    private javax.swing.JButton ChangeConnectionButton;
    private javax.swing.JLabel ConnectionLabel;
    private javax.swing.JLabel ConnectionLabel1;
    private javax.swing.JButton CreateObjectButton;
    private javax.swing.JLabel CurrentUserLabel;
    private javax.swing.JLabel DBNameLabel;
    private javax.swing.JButton DisconnectButton;
    private javax.swing.JTree JTreeObjects;
    private javax.swing.JTextArea ObjectDetails;
    private javax.swing.JButton SQLEditorButton;
    private javax.swing.JButton ShowDDLButton;
    private javax.swing.JButton SincronizacionButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    // End of variables declaration//GEN-END:variables
}
