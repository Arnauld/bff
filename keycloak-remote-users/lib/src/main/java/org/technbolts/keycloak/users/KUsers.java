package org.technbolts.keycloak.users;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;

public interface KUsers {

    UserModel findByUsername(RealmModel realm, String username);

    UserModel findByEmail(RealmModel realm, String email);

    boolean isPasswordValid(RealmModel realm, String username, String password);

    boolean updatePassword(RealmModel realm, String username, String newPassword);

    int count(RealmModel realm);

    List<UserModel> search(RealmModel realm, SearchCriteria criteria, int firstResult, int maxResults);

    UserModel addUser(RealmModel realm, String username);

    boolean removeUser(RealmModel realm, UserModel user);

    void flush();
}
