package org.technbolts.keycloak.users.jdbc;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.jboss.logging.Logger;
import org.json.JSONObject;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.technbolts.keycloak.users.KUserModel;
import org.technbolts.keycloak.users.KUsers;
import org.technbolts.keycloak.users.SearchCriteria;
import org.technbolts.utils.ConnectionCallback;
import org.technbolts.utils.PreparedStatementCallback;
import org.technbolts.utils.ResultSetMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class JdbcKUsers implements KUsers, AutoCloseable {
    private final Logger logger = Logger.getLogger(JdbcKUsers.class);

    private final KeycloakSession ksession;
    private final ComponentModel config;
    private final DataSource dataSource;
    private final List<KUserModel> tracked;

    public JdbcKUsers(KeycloakSession ksession, ComponentModel config, DataSource dataSource) {
        this.ksession = ksession;
        this.config = config;
        this.dataSource = dataSource;
        this.tracked = new ArrayList<>();
    }

    public void checkConfig() throws SQLException {
        try (Connection connection = openConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(config.get(Config.CONFIG_KEY_DB_VALIDATION_QUERY));
        }
    }

    private Connection openConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
    }

    @Override
    public void flush() {
        logger.infof("Flushing users");
        for (KUserModel model : tracked) {
            boolean modified = model.isModified();
            logger.infof("User %s marked as modified? %s", model.getId(), modified);
            if (modified) {
                persist(model);
            }
        }
    }

    @Override
    public UserModel findByEmail(RealmModel realm, String email) {
        try {
            return withConnection(c -> findFirst(c,
                    "select * from users where lower(data->>'email') = ?",
                    pstmt -> pstmt.setString(1, email.toLowerCase()),
                    mapUser(realm)));
        } catch (SQLException e) {
            logger.warnf(e, "Failed to find user '%s' by email", email);
            throw new RuntimeException("Failed to find user '" + email + "' by email", e);
        }
    }

    @Override
    public UserModel findByUsername(RealmModel realm, String username) {
        try {
            return withConnection(c -> findByUsername(realm, c, username));
        } catch (SQLException e) {
            logger.warnf(e, "Failed to find user '%s' by username", username);
            throw new RuntimeException("Failed to find user '" + username + "' by username", e);
        }
    }

    private UserModel findByUsername(RealmModel realm, Connection sql, String username) throws SQLException {
        return findFirst(
                sql,
                "select * from users where username = ?",
                pstmt -> pstmt.setString(1, username),
                mapUser(realm));
    }

    @Override
    public boolean isPasswordValid(RealmModel realm, String username, String password) {
        logger.infof("About to validate password of '%s' with '%s'", username, password);
        try {
            String hashedPassword = withConnection(c -> findFirst(c, "select password from users where username = ?",
                    pstmt -> pstmt.setString(1, username),
                    rs -> rs.getString(1)));
            if (hashedPassword != null) {
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
                if (!result.validFormat) {
                    logger.warnf("Invalid password hash format for user %s: %s", username, result.formatErrorMessage);
                }
                return result.verified;
            }
            return false;
        } catch (SQLException e) {
            logger.warnf(e, "Failed to validate password for user '%s'", username);
            throw new RuntimeException("Failed to validate password for user '" + username + "'", e);
        }
    }

    @Override
    public boolean updatePassword(RealmModel realm, String username, String newPassword) {
        String hashedPassword = BCrypt.withDefaults().hashToString(10, newPassword.toCharArray());
        logger.infof("About to update password of '%s' with hashed password '%s'", username, hashedPassword);
        try {
            return withConnection(c -> {
                try (PreparedStatement st = c.prepareStatement("update users set password = ? where username = ?")) {
                    st.setString(1, hashedPassword);
                    st.setString(2, username);
                    int nb = st.executeUpdate();
                    logger.infof("Password update for user %s: %s", username, nb);
                    if (nb == 0)
                        throw new RuntimeException("Failed to update password for user '" + username + "'");
                    return nb > 0;
                }
            });
        } catch (SQLException e) {
            logger.warnf(e, "Failed to update password for user '%s'", username);
            throw new RuntimeException("Failed to update password for user '" + username + "'", e);
        }
    }

    @Override
    public int count(RealmModel realm) {
        try {
            Integer v = withConnection(c -> findFirst(c, "select count(*) from users",
                    PreparedStatementCallback.NOOP,
                    rs -> rs.getInt(1)));
            if (v == null)
                return 0;
            return v;
        } catch (SQLException e) {
            logger.warnf(e, "Failed to count users");
            throw new RuntimeException("Failed to count users", e);
        }
    }

    @Override
    public List<UserModel> search(RealmModel realm, SearchCriteria criteria, int firstResult, int maxResults) {
        String sql = "select * from users";
        StringBuilder crit = new StringBuilder();
        List<String> params = new ArrayList<>();
        if (criteria.search() != null) {
            crit.append("username like ?");
            params.add(criteria.search());
        }

        if (crit.length() > 0)
            sql = sql + " where " + crit.toString();

        sql = sql + " order by id ";
        if (firstResult > 0)
            sql = sql + " offset " + firstResult;
        if (maxResults != Integer.MAX_VALUE)
            sql = sql + " limit " + maxResults;

        try {
            String effectiveSql = sql;
            logger.infof("Searching users using '%s'", effectiveSql);
            return withConnection(c ->
                    findList(c, effectiveSql,
                            pstmt -> {
                                for (int i = 0; i < params.size(); i++) {
                                    pstmt.setString(i + 1, params.get(i));
                                }
                            },
                            mapUser(realm)));
        } catch (SQLException e) {
            logger.warnf(e, "Failed to retrieve users");
            throw new RuntimeException("Failed to retrieve users", e);
        }
    }

    private void persist(KUserModel model) {
        try {
            withConnection(c -> {
                logger.infof("Persisting user %s", model.getId());
                try (PreparedStatement pstmt = c.prepareStatement("update users set data=?::jsonb, username=?, updated_at=? where id = ?")) {
                    pstmt.setObject(1, model.data().toString());
                    pstmt.setString(2, model.getUsername());
                    pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    pstmt.setLong(4, model.id());
                    pstmt.executeUpdate();
                    return true;
                }
            });
        } catch (SQLException e) {
            logger.warnf(e, "Failed to persist user '%s'", model.getId());
            throw new RuntimeException("Failed to persist user '" + model.getId() + "'", e);
        }
    }

    /*
        id             INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
        created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        username       VARCHAR(64) NOT NULL,
        password       VARCHAR(64),
        data           JSONB

     */
    private ResultSetMapper<UserModel> mapUser(RealmModel realm) {
        return rs -> track(new KUserModel(ksession, realm, config,
                rs.getLong("id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getString("username"),
                new JSONObject(rs.getString("data"))));
    }

    private UserModel track(KUserModel model) {
        tracked.add(model);
        return model;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        logger.infof("About to add user '%s'", username);
        try {
            return withConnection(c -> {
                try (PreparedStatement st = c.prepareStatement("insert into users (username) values (?)")) {
                    st.setString(1, username);
                    int nb = st.executeUpdate();
                    if (nb > 0) {
                        return findByUsername(realm, c, username);
                    }
                }
                throw new RuntimeException("Failed to add user '" + username + "'");
            });
        } catch (SQLException e) {
            logger.warnf(e, "Failed to add user '%s'", username);
            throw new RuntimeException("Failed to add user '" + username + "'", e);
        }

    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return false;
    }


    private <T> T withConnection(ConnectionCallback<T> func) throws SQLException {
        try (Connection c = openConnection()) {
            return func.withConnection(c);
        }
    }

    private <T> T findFirst(Connection c, String sql, PreparedStatementCallback prepareStmt, ResultSetMapper<T> mapper) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql)) {
            prepareStmt.prepare(st);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapper.map(rs);
            } else {
                return null;
            }
        }
    }

    private <T> List<T> findList(Connection c, String sql, PreparedStatementCallback prepareStmt, ResultSetMapper<T> mapper) throws SQLException {
        try (PreparedStatement st = c.prepareStatement(sql)) {
            prepareStmt.prepare(st);
            ResultSet rs = st.executeQuery();
            List<T> res = new ArrayList<>();
            while (rs.next()) {
                res.add(mapper.map(rs));
            }
            return res;
        }
    }
}
