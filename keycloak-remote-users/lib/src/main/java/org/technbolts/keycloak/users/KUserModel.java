package org.technbolts.keycloak.users;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.util.Date;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class KUserModel extends AbstractUserAdapter {

    private final String username;
    private final String email;
    private final String firstname;
    private final String lastname;
    private final Date birthdate;

    public KUserModel(KeycloakSession ksession, RealmModel realm, ComponentModel model,
                      String username,
                      String email,
                      String firstname,
                      String lastname,
                      Date birthdate) {
        super(ksession, realm, model);
        this.username = username;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.birthdate = birthdate;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getFirstName() {
        return firstname;
    }

    @Override
    public String getLastName() {
        return lastname;
    }

    @Override
    public String getEmail() {
        return email;
    }
}
