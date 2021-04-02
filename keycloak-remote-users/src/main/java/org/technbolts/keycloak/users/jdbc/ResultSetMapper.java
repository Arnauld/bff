package org.technbolts.keycloak.users.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public interface ResultSetMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
