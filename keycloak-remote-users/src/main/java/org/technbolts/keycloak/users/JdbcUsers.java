package org.technbolts.keycloak.users;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.sql.*;

public class JdbcUsers implements Users, AutoCloseable {
    private final Logger logger = Logger.getLogger(JdbcUsers.class);

    private final ComponentModel config;
    private final ConnectionProvider connectionProvider;

    public JdbcUsers(ComponentModel config, ConnectionProvider connectionProvider) {
        this.config = config;
        this.connectionProvider = connectionProvider;
    }

    public void checkConfig() throws SQLException {
        try (Connection connection = openConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(config.get(Config.CONFIG_KEY_DB_VALIDATION_QUERY));
        }
    }

    private Connection openConnection() throws SQLException {
        return connectionProvider.openConnection();
    }

    @Override
    public void close() {
        connectionProvider.dispose();
    }

    @Override
    public UserModel findByUsername(RealmModel realm, String username) {
        return findUnique(realm,
                "select * from users where username = ?",
                pstmt -> pstmt.setString(1, username));
    }

    private UserModel findUnique(RealmModel realm, String sql, PreparedStatementCallback prepareStmt) {
        try (Connection c = openConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            prepareStmt.prepare(st);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapUser(realm, rs);
            } else {
                return null;
            }
        } catch (Exception e) {

            return null;
        }
    }

    private UserModel mapUser(RealmModel realm, ResultSet rs) throws SQLException {
        CustomUser user = new CustomUser.Builder(ksession, realm, model, rs.getString("username"))
                .email(rs.getString("email"))
                .firstName(rs.getString("firstName"))
                .lastName(rs.getString("lastName"))
                .birthDate(rs.getDate("birthDate"))
                .build();

        return user;
    }

}
