package org.technbolts.utils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public interface ConnectionCallback<T> {
    T withConnection(Connection connection) throws SQLException;
}
