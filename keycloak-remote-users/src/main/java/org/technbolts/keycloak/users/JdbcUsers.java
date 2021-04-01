package org.technbolts.keycloak.users;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.keycloak.component.ComponentModel;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class JdbcUsers implements Users, AutoCloseable {
    private final ComponentModel config;
    private HikariDataSource dataSource;

    public JdbcUsers(ComponentModel config) {
        this.config = config;
    }

    public void checkConfig() throws SQLException {
        try (Connection connection = openConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(config.get(Config.CONFIG_KEY_DB_VALIDATION_QUERY));
        }
    }

    synchronized Connection openConnection() throws SQLException {
        if (dataSource == null) {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(config.get(Config.CONFIG_KEY_JDBC_URL));
            cfg.setUsername(config.get(Config.CONFIG_KEY_DB_USERNAME));
            cfg.setPassword(config.get(Config.CONFIG_KEY_DB_PASSWORD));
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(0);
            cfg.setIdleTimeout(TimeUnit.MINUTES.toMillis(1));
            cfg.addDataSourceProperty("cachePrepStmts", "true");
            cfg.addDataSourceProperty("prepStmtCacheSize", "250");
            cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(cfg);
        }
        return dataSource.getConnection();
    }

    @Override
    public void close() throws Exception {
        if(dataSource!=null) {
            dataSource.close();
        }
    }
}
