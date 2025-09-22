/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package databasemanager_sqlanywhere_andreaquin;

import java.sql.Connection;
import java.sql.SQLException;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author Andrea Quin
 */
public class CreateObject extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CreateObject.class.getName());
    private JComboBox<String> typeCombo;
    private JTextField nameField;
    private JPanel dynamicPanel;
    private JButton createButton, cancelButton;

    private JTextField seqStartField;
    private JTextField seqIncrementField;
    private JTextField seqMinField;
    private JTextField seqMaxField;
    private JCheckBox seqCycleCheck;

    private JTextArea viewSqlArea;
    private JTextArea triggerSqlArea;

    private ConnectionManager connectionManager;
    private ManagerFrame manager;

    private JTable columnTable;

    public CreateObject(ConnectionManager connectionManager, ManagerFrame manager) {

        this.connectionManager = connectionManager;
        this.manager = manager;

        initComponents();
        initCustomComponents();
        
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                manager.setVisible(true);
            }
        });
    }
    
    private void initCustomComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        topPanel.add(new JLabel("Object Type:"));
        typeCombo = new JComboBox<>(new String[]{"TABLE", "VIEW", "SEQUENCE", "TRIGGER"});
        topPanel.add(typeCombo);

        topPanel.add(new JLabel("Object Name:"));
        nameField = new JTextField();
        topPanel.add(nameField);

        add(topPanel, BorderLayout.NORTH);

        //ICON
        ImageIcon icon = new ImageIcon(main.class.getResource("/resources/Icon.png"));
        this.setIconImage(icon.getImage());

        dynamicPanel = new JPanel(new CardLayout());
        dynamicPanel.add(createTablePanel(), "TABLE");
        dynamicPanel.add(createViewPanel(), "VIEW");
        dynamicPanel.add(createSequencePanel(), "SEQUENCE");
        dynamicPanel.add(createTriggerPanel(), "TRIGGER");
        add(dynamicPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        createButton = new JButton("Create");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        typeCombo.addActionListener(e -> {
            CardLayout cl = (CardLayout) (dynamicPanel.getLayout());
            cl.show(dynamicPanel, (String) typeCombo.getSelectedItem());
        });

        createButton.addActionListener(e -> onCreate());
        cancelButton.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel prefixLabel = new JLabel("CREATE VIEW <name> AS");
        panel.add(prefixLabel, BorderLayout.NORTH);

        viewSqlArea = new JTextArea(10, 40);
        panel.add(new JScrollPane(viewSqlArea), BorderLayout.CENTER);

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateLabel() {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    prefixLabel.setText("CREATE VIEW <name> AS");
                } else {
                    prefixLabel.setText("CREATE VIEW " + name + " AS");
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }
        });

        return panel;
    }


private JPanel createSequencePanel() {
    JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

    panel.add(new JLabel("Start With:"));
    seqStartField = new JTextField("1"); 
    panel.add(seqStartField);

    panel.add(new JLabel("Increment By:"));
    seqIncrementField = new JTextField("1");
    panel.add(seqIncrementField);

    panel.add(new JLabel("Min Value:"));
    seqMinField = new JTextField();
    panel.add(seqMinField);

    panel.add(new JLabel("Max Value:"));
    seqMaxField = new JTextField();
    panel.add(seqMaxField);

    panel.add(new JLabel("Cycle:"));
    seqCycleCheck = new JCheckBox();
    panel.add(seqCycleCheck);

    return panel;
}

    private JPanel createTriggerPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel prefixLabel = new JLabel("CREATE TRIGGER <name>");
        panel.add(prefixLabel, BorderLayout.NORTH);

        triggerSqlArea = new JTextArea(10, 40);
        panel.add(new JScrollPane(triggerSqlArea), BorderLayout.CENTER);

        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void updateLabel() {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    prefixLabel.setText("CREATE TRIGGER <name>");
                } else {
                    prefixLabel.setText("CREATE TRIGGER " + name);
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateLabel();
            }
        });

        return panel;
    }

    private JPanel createTablePanel() {
        String[] columnNames = {"Column Name", "Data Type", "Primary Key", "Nullable", "Default"};
        Object[][] data = {
            {"id", "INT", Boolean.TRUE, Boolean.FALSE, null}
        };

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(data, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2 || columnIndex == 3) {
                    return Boolean.class;
                }
                return String.class;
            }
        };

        columnTable = new JTable(model);
        columnTable.setFillsViewportHeight(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(columnTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addRow = new JButton("Add Column");
        JButton removeRow = new JButton("Remove Column");

        addRow.addActionListener(e -> model.addRow(new Object[]{"", "VARCHAR(100)", Boolean.FALSE, Boolean.TRUE, null}));
        removeRow.addActionListener(e -> {
            int selected = columnTable.getSelectedRow();
            if (selected != -1) {
                model.removeRow(selected);
            }
        });

        buttonPanel.add(addRow);
        buttonPanel.add(removeRow);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void onCreate() {
        String type = (String) typeCombo.getSelectedItem();
        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Object name cannot be empty");
            return;
        }

        String ddlString = null;

        switch (type) {
            case "TABLE":
                javax.swing.table.TableModel model = columnTable.getModel();
                StringBuilder cols = new StringBuilder();
                for (int i = 0; i < model.getRowCount(); i++) {
                    String colName = model.getValueAt(i, 0).toString().trim();
                    String colType = model.getValueAt(i, 1).toString().trim();
                    boolean pk = (Boolean) model.getValueAt(i, 2);
                    boolean nullable = (Boolean) model.getValueAt(i, 3);
                    String defVal = (model.getValueAt(i, 4) != null) ?
                            model.getValueAt(i, 4).toString().trim() : "";

                    if (colName.isEmpty() || colType.isEmpty()) continue;
                    if (cols.length() > 0) cols.append(", ");

                    cols.append(colName).append(" ").append(colType);
                    if (pk) {
                        cols.append(" PRIMARY KEY");
                    }
                    if (!nullable) {
                        cols.append(" NOT NULL");
                    }
                    if (!defVal.isEmpty()) {
                        cols.append(" DEFAULT ").append(defVal);
                    }
                }
                ddlString = "CREATE TABLE " + name + " (" + cols.toString() + ")";
                break;

            case "VIEW":
                String viewQuery = viewSqlArea.getText().trim();
                if (viewQuery.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "View query cannot be empty!");
                    return;
                }
                ddlString = "CREATE VIEW " + name + " AS\n" + viewQuery;
                break;

            case "SEQUENCE":
                String start = seqStartField.getText().trim();
                String increment = seqIncrementField.getText().trim();
                String min = seqMinField.getText().trim();
                String max = seqMaxField.getText().trim();
                boolean cycle = seqCycleCheck.isSelected();

                if (start.isEmpty() || increment.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Start and Increment cannot be empty!");
                    return;
                }

                ddlString = "CREATE SEQUENCE " + name
                        + " START WITH " + start
                        + " INCREMENT BY " + increment;

                if (!min.isEmpty()) {
                    ddlString += " MINVALUE " + min;
                }
                if (!max.isEmpty()) {
                    ddlString += " MAXVALUE " + max;
                }
                if (cycle) {
                    ddlString += " CYCLE";
                }
                break;

            case "TRIGGER":
                String triggerBody = triggerSqlArea.getText().trim();
                if (triggerBody.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Trigger body cannot be empty!");
                    return;
                }
                ddlString = "CREATE TRIGGER " + name + " " + triggerBody;
                break;
        }

        if (ddlString == null) {
            JOptionPane.showMessageDialog(this, "No DDL generated for type: " + type);
            return;
        }

        try {
            Connection conn = connectionManager.getActiveConnection();
            conn.createStatement().executeUpdate(ddlString);
            JOptionPane.showMessageDialog(this, type + " created successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "❌ Error creating " + type + ": " + ex.getMessage());
        }
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
