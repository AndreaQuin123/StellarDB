package databasemanager_sqlanywhere_andreaquin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

//json
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConnectionManager {

    private Map<String, Connection> connections = new HashMap<>();
    private Map<String, ConnectionInfo> savedConnections = new HashMap<>();
    private String activeKey = null;

    private final File file = new File("connections.json");

    public ConnectionManager() {
        loadConnections();
    }

    public static class ConnectionInfo {

        int port;
        String database, user, password;
        boolean type;

        ConnectionInfo(int port, String database, String user, String password) {
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
            this.type = true; //true is SQLANYWHERE, false is POSTGRES
        }
    }

    public boolean hasActiveConnection() {
        return activeKey != null && connections.containsKey(activeKey);
    }

    public void addConnection(String key, ConnectionInfo info) throws SQLException {
        if (info.type) { // SQL Anywhere
            String url = "jdbc:sqlanywhere:Port=" + info.port + ";DatabaseName=" + info.database;
            Connection conn = DriverManager.getConnection(url, info.user, info.password);
            connections.put(key, conn);

            activeKey = key;

        } else { // Postgres
            String url = "jdbc:postgresql://localhost:" + info.port + "/" + info.database;
            Connection conn = DriverManager.getConnection(url, info.user, info.password);
            connections.put(key, conn);
        }

        savedConnections.put(key, info);

        saveConnections();
    }

    public Connection getActiveConnection() {
        return connections.get(activeKey);
    }

    public String getSavedActiveKey() {
        return activeKey;
    }

    public ConnectionInfo getSavedConnectionInfo(String key) {
        return savedConnections.get(key);
    }

    public void setActiveConnection(String key) throws SQLException {
        if (!savedConnections.containsKey(key)) {
            throw new IllegalArgumentException("Connection not found: " + key);
        }

        if (!connections.containsKey(key)) {
            ConnectionInfo info = savedConnections.get(key);
            addConnection(key, info);
        }

        activeKey = key;
        saveConnections();
    }

    public String[] getConnectionKeys() {
        return savedConnections.keySet().toArray(new String[0]);
    }

    public void disconnectActiveConnection() {
        if (activeKey == null) {
            return;
        }
        try {
            Connection conn = connections.get(activeKey);
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connections.remove(activeKey);
        activeKey = null;
        saveConnections();
    }

    private void saveConnections() {
        JSONObject root = new JSONObject();
        JSONObject conns = new JSONObject();

        for (Map.Entry<String, ConnectionInfo> e : savedConnections.entrySet()) {
            ConnectionInfo c = e.getValue();
            JSONObject info = new JSONObject();
            info.put("port", c.port);
            info.put("database", c.database);
            info.put("user", c.user);
            info.put("password", c.password);
            info.put("type", c.type); // true = SQL Anywhere, false = Postgres
            conns.put(e.getKey(), info);
        }

        root.put("connections", conns);
        root.put("active", activeKey != null ? activeKey : "");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(root.toJSONString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private void loadConnections() {
        if (!file.exists()) {
            return;
        }

        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(file)) {
            JSONObject root = (JSONObject) parser.parse(reader);
            JSONObject conns = (JSONObject) root.get("connections");

            for (Object keyObj : conns.keySet()) {
                String key = (String) keyObj;
                JSONObject info = (JSONObject) conns.get(key);

                int port = ((Long) info.get("port")).intValue();
                String db = (String) info.get("database");
                String user = (String) info.get("user");
                String pass = (String) info.get("password");
                Object typeObj = info.get("type");
                boolean type = (typeObj != null) ? (Boolean) typeObj : true; //error of null
                ConnectionInfo ci = new ConnectionInfo(port, db, user, pass);
                ci.type = type;
                savedConnections.put(key, ci);
            }

            String active = (String) root.get("active");
            activeKey = (active != null && !active.isEmpty()) ? active : null;

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
