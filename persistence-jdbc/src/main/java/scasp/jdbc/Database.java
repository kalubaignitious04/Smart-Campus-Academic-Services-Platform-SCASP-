package scasp.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.sql.DataSource;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

/** Creates the pooled DataSource and runs the repeatable SQL scripts (schema.sql, sample-data.sql). */
public final class Database {
    private Database() {}

    /** Connection pool from the Tomcat runtime (DBCP). */
    public static BasicDataSource pooled(String url, int maxConnections) {
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl(url);
        ds.setUsername("sa");
        ds.setPassword("");
        ds.setMaxTotal(maxConnections);
        ds.setMaxWaitMillis(5000);
        return ds;
    }

    /** Creates tables if missing and loads sample data into an empty database. */
    public static void init(DataSource ds, boolean loadSamples) {
        try (Connection c = ds.getConnection()) {
            runScript(c, "/database/schema.sql");
            if (loadSamples) {
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                    rs.next();
                    if (rs.getInt(1) == 0) runScript(c, "/database/sample-data.sql");
                }
            }
        } catch (SQLException e) { throw new StoreException(e); }
    }

    private static void runScript(Connection c, String resource) throws SQLException {
        String sql;
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Missing resource " + resource);
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException(e); }
        StringBuilder clean = new StringBuilder();
        for (String line : sql.split("\n")) if (!line.trim().startsWith("--")) clean.append(line).append('\n');
        try (Statement st = c.createStatement()) {
            for (String stmt : clean.toString().split(";\\s*\n")) if (!stmt.isBlank()) st.execute(stmt);
        }
    }
}
