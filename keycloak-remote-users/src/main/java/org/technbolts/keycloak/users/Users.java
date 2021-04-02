package org.technbolts.keycloak.users;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public interface Users {
    UserModel findByUsername(RealmModel realm, String username);

    UserModel findByEmail(RealmModel realm, String email);
}
