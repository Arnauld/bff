package org.technbolts.keycloak.users;

import org.keycloak.component.ComponentModel;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcUsers {
    private final ComponentModel config;

    public JdbcUsers(ComponentModel config) {
        this.config = config;
    }

    public void checkConfig() throws SQLException {
        try(Connection connection = openConnection();
            Statement stmt = connection.createStatement()) {
            stmt.execute(config.get(Config.CONFIG_KEY_DB_VALIDATION_QUERY));
        }
    }

    private Connection openConnection() {
        return null;
    }
}
