/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package databasemanager_sqlanywhere_andreaquin;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.layout.mxOrganicLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.sql.*;
import java.util.*;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;



public class DiagramFrame extends JPanel {
    
    // style of the boxes
    String tableStyle = "shape=rectangle;" + "rounded=1;" + "fillColor=#dae8fc;" +
                        "strokeColor=#6c8ebf;" + "fontColor=#000000;" + "verticalAlign=top;" +
                        "align=left;" + "spacingTop=4;" + "spacingLeft=4;" + "spacingRight=4;" + "spacingBottom=4;";

    public DiagramFrame(Connection conn) {
        super(new BorderLayout());

        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();
        try {
            Map<String, Object> tableNodes = new HashMap<>();
            Map<String, Set<String>> pkMap = new HashMap<>();

            Statement st = conn.createStatement();

            //tables
            ResultSet rsTables = st.executeQuery("""
                SELECT t.table_name
                FROM systab t
                JOIN sysuser u ON t.creator = u.user_id
                WHERE t.table_type = 1
                  AND u.user_name NOT IN ('SYS', 'dbo', 'PUBLIC')
                  AND t.table_name NOT LIKE 'rs_%'
                ORDER BY t.table_name
            """);

            while (rsTables.next()) {
                String tableName = rsTables.getString("table_name");

                //pks
                ResultSet rsPK = conn.getMetaData().getPrimaryKeys(null, null, tableName);
                Set<String> pkCols = new HashSet<>();
                while (rsPK.next()) {
                    pkCols.add(rsPK.getString("COLUMN_NAME"));
                }
                pkMap.put(tableName, pkCols);

                //columns
                ResultSet rsCols = conn.getMetaData().getColumns(null, null, tableName, null);
                StringBuilder label = new StringBuilder(tableName).append("\n----------------");
                while (rsCols.next()) {
                    String colName = rsCols.getString("COLUMN_NAME");
                    String colType = rsCols.getString("TYPE_NAME");
                    if (pkCols.contains(colName)) {
                        label.append("\n* ").append(colName).append(" : ").append(colType);
                    } else {
                        label.append("\n").append(colName).append(" : ").append(colType);
                    }
                }

                //vertex
                Object v = graph.insertVertex(
                        parent, null, label.toString(), 0, 0, 180, 120,
                        tableStyle
                );
                tableNodes.put(tableName, v);
            }

            //fks
            for (String tableName : tableNodes.keySet()) {
                ResultSet fks = conn.getMetaData().getImportedKeys(conn.getCatalog(), null, tableName);
                while (fks.next()) {
                    String pkTable = fks.getString("PKTABLE_NAME");
                    String fkColumn = fks.getString("FKCOLUMN_NAME");
                    String pkColumn = fks.getString("PKCOLUMN_NAME");

                    if (tableNodes.containsKey(pkTable) && tableNodes.containsKey(tableName)) {
                        String edgeLabel = fkColumn + " → " + pkColumn;
                        graph.insertEdge(parent, null, edgeLabel,
                                tableNodes.get(tableName), tableNodes.get(pkTable));
                    }
                }
            }

            //layout
            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
            layout.setOrientation(SwingConstants.WEST);
            layout.setIntraCellSpacing(50);
            layout.setInterRankCellSpacing(80);
            layout.setParallelEdgeSpacing(20);
            layout.execute(parent);

        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setConnectable(false);
        graphComponent.getGraphControl().setBackground(Color.WHITE);

        this.add(new JScrollPane(graphComponent), BorderLayout.CENTER);
    }
}
