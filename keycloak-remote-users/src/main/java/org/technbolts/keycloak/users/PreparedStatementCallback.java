package org.technbolts.keycloak.users;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCallback {
    void prepare(PreparedStatement pstmt) throws SQLException;
}
