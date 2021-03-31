package org.technbolts.keycloak.users;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class CustomUserStorageProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, UserQueryProvider {

    private final Logger logger = Logger.getLogger(CustomUserStorageProvider.class);
    private final KeycloakSession ksession;
    private final ComponentModel model;
    private final JdbcUsers users;

    public CustomUserStorageProvider(KeycloakSession ksession, ComponentModel model) {
        this.ksession = ksession;
        this.model = model;
        this.users = new JdbcUsers(model);
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        logger.infof("Lookup user by id: '%s'", id);
        return null;
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        logger.infof("Lookup user by username: '%s'", username);
        return null;
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        logger.infof("Lookup user by email: '%s'", email);
        return null;
    }
}
