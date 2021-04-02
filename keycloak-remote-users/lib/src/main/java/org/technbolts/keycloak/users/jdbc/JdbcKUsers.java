package org.technbolts.keycloak.users.jdbc;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.technbolts.keycloak.users.KUserModel;
import org.technbolts.keycloak.users.KUsers;
import org.technbolts.keycloak.users.SearchCriteria;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcKUsers implements KUsers, AutoCloseable {
    private final Logger logger = Logger.getLogger(JdbcKUsers.class);

    private final KeycloakSession ksession;
    private final ComponentModel config;
    private final DataSource dataSource;

    public JdbcKUsers(KeycloakSession ksession, ComponentModel config, DataSource dataSource) {
        this.ksession = ksession;
        this.config = config;
        this.dataSource = dataSource;
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
    public UserModel findByEmail(RealmModel realm, String email) {
        return findUnique(
                "select * from users where LOWER(email) = ?",
                pstmt -> pstmt.setString(1, email.toLowerCase()),
                mapUser(realm));
    }

    @Override
    public UserModel findByUsername(RealmModel realm, String username) {
        return findUnique(
                "select * from users where username = ?",
                pstmt -> pstmt.setString(1, username),
                mapUser(realm));
    }

    private <T> T findUnique(String sql, PreparedStatementCallback prepareStmt, ResultSetMapper<T> mapper) {
        try (Connection c = openConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            prepareStmt.prepare(st);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return mapper.map(rs);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.warnf(e, "Failed to execute query '%s'", sql);
            return null;
        }
    }

    private <T> List<T> findList(String sql, PreparedStatementCallback prepareStmt, ResultSetMapper<T> mapper) {
        try (Connection c = openConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            prepareStmt.prepare(st);
            ResultSet rs = st.executeQuery();
            List<T> res = new ArrayList<>();
            while (rs.next()) {
                res.add(mapper.map(rs));
            }
            return res;
        } catch (Exception e) {
            logger.warnf(e, "Failed to execute query '%s'", sql);
            return null;
        }
    }

    @Override
    public boolean isPasswordValid(RealmModel realm, String username, String password) {
        logger.infof("About to validate password of '%s' with '%s'", username, password);
        String pwd = findUnique("select password from users where username = ?",
                pstmt -> pstmt.setString(1, username),
                rs -> rs.getString(1));
        return pwd != null && pwd.equalsIgnoreCase(password);
    }

    @Override
    public boolean updatePassword(RealmModel realm, String username, String newPassword) {
        logger.infof("About to update password of '%s' with '%s'", username, newPassword);
        try (Connection c = openConnection();
             PreparedStatement st = c.prepareStatement("update users set password = ? where username = ?")) {
            st.setString(1, username);
            st.setString(2, newPassword);
            int nb = st.executeUpdate();
            return nb > 0;
        } catch (Exception e) {
            logger.warnf(e, "Failed to update password for username '%s'", username);
            return false;
        }
    }

    @Override
    public int count(RealmModel realm) {
        Integer v = findUnique("select count(*) from users",
                PreparedStatementCallback.NOOP,
                rs -> rs.getInt(1));
        if (v == null)
            return 0;
        return v;
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

        logger.infof("Searching users using '%s'", sql);

        return findList(sql, pstmt -> {
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setString(i + 1, params.get(i));
                    }
                },
                mapUser(realm));
    }

    private ResultSetMapper<UserModel> mapUser(RealmModel realm) {
        return rs -> new KUserModel(ksession, realm, config,
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("firstname"),
                rs.getString("lastname"));
    }

}
