package databasemanager_sqlanywhere_andreaquin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

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

        ConnectionInfo(int port, String database, String user, String password) {
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
        }
    }

    public boolean hasActiveConnection() {
        return activeKey != null && connections.containsKey(activeKey);
    }

    public void addConnection(String key, ConnectionInfo info) throws SQLException {
        String url = "jdbc:sqlanywhere:Port=" + info.port + ";DatabaseName=" + info.database;
        Connection conn = DriverManager.getConnection(url, info.user, info.password);

        connections.put(key, conn);
        savedConnections.put(key, info);
        activeKey = key;
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
        if (activeKey == null) return;
        try {
            Connection conn = connections.get(activeKey);
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connections.remove(activeKey);
        activeKey = null;
        saveConnections();
    }

    private void saveConnections() {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("{");
            pw.println("  \"connections\": {");
            int count = 0;
            for (Map.Entry<String, ConnectionInfo> e : savedConnections.entrySet()) {
                ConnectionInfo c = e.getValue();
                pw.printf("    \"%s\": {\"port\":%d,\"database\":\"%s\",\"user\":\"%s\",\"password\":\"%s\"}%s\n",
                        e.getKey(), c.port, c.database, c.user, c.password,
                        (++count < savedConnections.size()) ? "," : "");
            }
            pw.println("  },");
            pw.println("  \"active\": \"" + (activeKey != null ? activeKey : "") + "\"");
            pw.println("}");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

 private void loadConnections() {
    if (!file.exists()) return;

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());
        String json = sb.toString();

        int connsStart = json.indexOf("\"connections\"") + 14;
        int connsEnd = json.indexOf("}", connsStart);
        if (connsStart < 14 || connsEnd < connsStart) return;
        String connsJson = json.substring(connsStart, connsEnd + 1);

        String[] entries = connsJson.split("\\},\\s*\"");
        for (String entry : entries) {
            if (!entry.contains("{")) continue;

            int keyStart = entry.indexOf("\"");
            int keyEnd = entry.indexOf("\"", keyStart + 1);
            String key = entry.substring(keyStart + 1, keyEnd);

            int bodyStart = entry.indexOf("{");
            int bodyEnd = entry.indexOf("}", bodyStart);
            if (bodyStart < 0 || bodyEnd < 0) continue;
            String body = entry.substring(bodyStart + 1, bodyEnd);

            Map<String, String> map = new HashMap<>();
            for (String part : body.split(",")) {
                String[] kv = part.split(":", 2);
                if (kv.length < 2) continue;
                map.put(kv[0].replace("\"", "").trim(), kv[1].replace("\"", "").trim());
            }

            int port = Integer.parseInt(map.getOrDefault("port", "0"));
            String db = map.getOrDefault("database", "");
            String user = map.getOrDefault("user", "");
            String pass = map.getOrDefault("password", "");

            savedConnections.put(key, new ConnectionInfo(port, db, user, pass));
        }

        // Active key
        int aStart = json.indexOf("\"active\"") + 9;
        int aEnd = json.indexOf("\"", aStart + 1);
        if (aStart > 8 && aEnd > aStart) {
            activeKey = json.substring(aStart, aEnd).trim();
            if (activeKey.isEmpty()) activeKey = null;
        }

    } catch (IOException | NumberFormatException e) {
        e.printStackTrace();
    }
}

}
