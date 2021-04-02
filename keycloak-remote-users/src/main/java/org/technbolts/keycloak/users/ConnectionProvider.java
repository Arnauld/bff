package org.technbolts.keycloak.users;

import java.sql.Connection;

public interface ConnectionProvider {
    Connection openConnection();

    void dispose();
}
