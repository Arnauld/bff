package org.technbolts.keycloak.users.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCallback {
    PreparedStatementCallback NOOP = pstmt -> {
    };

    void prepare(PreparedStatement pstmt) throws SQLException;
}
