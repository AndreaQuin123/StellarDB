/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package databasemanager_sqlanywhere_andreaquin;

import java.sql.Connection;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author Andrea Quin
 */
public class SQLEditorFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SQLEditorFrame.class.getName());

    private ConnectionManager connectionManager;
    private ManagerFrame manager;

    public SQLEditorFrame(ConnectionManager connectionManager, ManagerFrame manager) {
        this.manager = manager;
        this.connectionManager = connectionManager;
        initComponents();

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Database Objects");
        JTreeObjects.setModel(new DefaultTreeModel(root));
        loadDatabaseObjects();

        //ICON
        ImageIcon icon = new ImageIcon(main.class.getResource("/resources/Icon.png"));
        this.setIconImage(icon.getImage());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                manager.setVisible(true); //regresar al ManagerFrame
            }
        });
    }

    private Connection getActiveConnection() {
        return connectionManager.getActiveConnection();
    }

    private void runSQL() {
        String sql = SQLEditor.getText().trim();
        if (sql.isEmpty()) {
            ResultText.setText("⚠️ No SQL entered.");
            return;
        }

        Connection conn = connectionManager.getActiveConnection();
        if (conn == null) {
            ResultText.setText("❌ No active connection!");
            return;
        }

        try (Statement st = conn.createStatement()) {
            boolean hasResultSet = st.execute(sql);

            StringBuilder output = new StringBuilder();

            if (hasResultSet) {
                try (ResultSet rs = st.getResultSet()) {
                    output.append(resultSetToString(rs));
                }
            } else {
                int count = st.getUpdateCount();
                if (count >= 0) {
                    output.append("✅ Rows affected: ").append(count).append("\n");
                } else {
                    output.append("✅ Statement executed successfully.\n");
                }
            }

            ResultText.setText(output.toString());

        } catch (SQLException e) {
            ResultText.setText("❌ SQL Error: " + e.getMessage());
        }

        ResultText.setVisible(true);
        ResultText.revalidate();
        ResultText.repaint();
    }

    private String resultSetToString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        for (int i = 1; i <= colCount; i++) {
            sb.append(String.format("%-20s", meta.getColumnName(i)));
        }
        sb.append("\n").append("-".repeat(colCount * 20)).append("\n");

        while (rs.next()) {
            for (int i = 1; i <= colCount; i++) {
                sb.append(String.format("%-20s", rs.getString(i)));
            }
            sb.append("\n");
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

        JTreeObjects.setModel(new DefaultTreeModel(root));
        JTreeObjects.expandRow(0);
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
        RunSQLButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        JTreeObjects = new javax.swing.JTree();
        jScrollPane3 = new javax.swing.JScrollPane();
        ResultText = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        SQLEditor = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(null);

        RunSQLButton.setText("RUN");
        RunSQLButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                RunSQLButtonMouseClicked(evt);
            }
        });
        jPanel1.add(RunSQLButton);
        RunSQLButton.setBounds(1009, 357, 110, 40);

        jScrollPane1.setBackground(new java.awt.Color(0, 0, 0));
        jScrollPane1.setForeground(new java.awt.Color(0, 0, 0));

        JTreeObjects.setBackground(new java.awt.Color(255, 255, 255));
        JTreeObjects.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        JTreeObjects.setFont(new java.awt.Font("Gadugi", 1, 14)); // NOI18N
        JTreeObjects.setForeground(new java.awt.Color(0, 0, 0));
        jScrollPane1.setViewportView(JTreeObjects);

        jPanel1.add(jScrollPane1);
        jScrollPane1.setBounds(40, 30, 320, 620);

        ResultText.setEditable(false);
        ResultText.setBackground(new java.awt.Color(255, 255, 255));
        ResultText.setColumns(20);
        ResultText.setForeground(new java.awt.Color(0, 0, 0));
        ResultText.setRows(5);
        ResultText.setBorder(null);
        jScrollPane3.setViewportView(ResultText);

        jPanel1.add(jScrollPane3);
        jScrollPane3.setBounds(380, 430, 750, 220);

        SQLEditor.setBackground(new java.awt.Color(255, 255, 255));
        SQLEditor.setColumns(20);
        SQLEditor.setForeground(new java.awt.Color(0, 0, 0));
        SQLEditor.setRows(5);
        SQLEditor.setBorder(null);
        jScrollPane2.setViewportView(SQLEditor);

        jPanel1.add(jScrollPane2);
        jScrollPane2.setBounds(380, 30, 750, 380);

        jLabel1.setBackground(new java.awt.Color(204, 204, 255));
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/SQLEditor.png"))); // NOI18N
        jPanel1.add(jLabel1);
        jLabel1.setBounds(0, 0, 1200, 675);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1200, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 675, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void RunSQLButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RunSQLButtonMouseClicked
        runSQL();
    }//GEN-LAST:event_RunSQLButtonMouseClicked

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree JTreeObjects;
    private javax.swing.JTextArea ResultText;
    private javax.swing.JButton RunSQLButton;
    private javax.swing.JTextArea SQLEditor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    // End of variables declaration//GEN-END:variables
}
