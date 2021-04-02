package org.technbolts.keycloak.users;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class UsersStorageProvider
        implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, UserQueryProvider {

    private final Logger logger = Logger.getLogger(UsersStorageProvider.class);
    private final KeycloakSession ksession;
    private final ComponentModel model;
    private final Users users;

    public UsersStorageProvider(KeycloakSession ksession, ComponentModel model, Users users) {
        this.ksession = ksession;
        this.model = model;
        this.users = users;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        logger.infof("::supportsCredentialType '%s'", credentialType);
        return PasswordCredentialModel.TYPE.endsWith(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.infof("::isConfiguredFor(realm='%s', user='%s', credentialType='%s'", realm.getName(), user.getUsername(), credentialType);
        // In our case, password is the only type of credential,
        // so we allways return 'true' if this is the credentialType
        return supportsCredentialType(credentialType);
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        logger.infof("Lookup user by id: '%s'", id);
        StorageId sid = new StorageId(id);
        return users.findByUsername(realmModel, sid.getExternalId());
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        logger.infof("Lookup user by username: '%s'", username);
        return users.findByUsername(realmModel, username);
    }

    /**
     * {{@link UserLookupProvider}}
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        logger.infof("Lookup user by email: '%s'", email);
        return users.findByEmail(realmModel, email);
    }
}
