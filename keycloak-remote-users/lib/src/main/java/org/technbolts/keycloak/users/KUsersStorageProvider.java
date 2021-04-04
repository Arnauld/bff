package org.technbolts.keycloak.users;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class KUsersStorageProvider
        implements
        UserStorageProvider,
        UserLookupProvider,
        UserQueryProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        UserRegistrationProvider {

    private final Logger logger = Logger.getLogger(KUsersStorageProvider.class);
    private final KeycloakSession ksession;
    private final ComponentModel model;
    private final KUsers kUsers;

    public KUsersStorageProvider(KeycloakSession ksession, ComponentModel model, KUsers kUsers) {
        this.ksession = ksession;
        this.model = model;
        this.kUsers = kUsers;
    }

    /**
     * {@link CredentialInputValidator}
     */
    @Override
    public boolean supportsCredentialType(String credentialType) {
        logger.infof("::supportsCredentialType '%s'", credentialType);
        return PasswordCredentialModel.TYPE.endsWith(credentialType);
    }


    /**
     * {@link CredentialInputValidator}
     */
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        logger.infof("::isConfiguredFor(realm='%s', user='%s', credentialType='%s'", realm.getName(), user.getUsername(), credentialType);
        // In our case, password is the only type of credential,
        // so we allways return 'true' if this is the credentialType
        return supportsCredentialType(credentialType);
    }

    /**
     * {@link CredentialInputValidator}
     */
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        StorageId sid = new StorageId(user.getId());
        String username = sid.getExternalId();
        if (kUsers.isPasswordValid(realm, username, input.getChallengeResponse())) {
            // ksession.userCredentialManager().updateCredential(realm, user, input);
            return true;
        }
        return false;
    }

    /**
     * {@link CredentialInputUpdater}
     */
    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) {
            return false;
        }

        StorageId sid = new StorageId(user.getId());
        String username = sid.getExternalId();

        return kUsers.updatePassword(realm, username, input.getChallengeResponse());
    }

    /**
     * {@link CredentialInputUpdater}
     */
    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        // Not needed
    }

    /**
     * {@link CredentialInputUpdater}
     */
    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.emptySet();
    }

    /**
     * {@link UserLookupProvider}
     */
    @Override
    public UserModel getUserById(String id, RealmModel realmModel) {
        logger.infof("Lookup user by id: '%s'", id);
        StorageId sid = new StorageId(id);
        return kUsers.findByUsername(realmModel, sid.getExternalId());
    }

    /**
     * {@link UserLookupProvider}
     */
    @Override
    public UserModel getUserByUsername(String username, RealmModel realmModel) {
        logger.infof("Lookup user by username: '%s'", username);
        return kUsers.findByUsername(realmModel, username);
    }

    /**
     * {@link UserLookupProvider}
     */
    @Override
    public UserModel getUserByEmail(String email, RealmModel realmModel) {
        logger.infof("Lookup user by email: '%s'", email);
        return kUsers.findByEmail(realmModel, email);
    }

    /**
     * {@link org.keycloak.provider.Provider}
     */
    @Override
    public void close() {
        kUsers.flush();
        logger.infof("Closing provider");
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public int getUsersCount(RealmModel realm) {
        return kUsers.count(realm);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return kUsers.search(realm, new SearchCriteria(), firstResult, maxResults);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, 100);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        return kUsers.search(realm, new SearchCriteria().withSearch(search), firstResult, maxResults);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, 100);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        return kUsers.search(realm,
                new SearchCriteria()
                        .withUsername(params.get("first"))
                        .withLastname(params.get("last"))
                        .withEmail(params.get("email"))
                        .withEmail(params.get("email"))
                //.withEnabled(params.get("enabled")),
                , firstResult, maxResults);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, 0, Integer.MAX_VALUE);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return kUsers.search(realm,
                new SearchCriteria().withGroupId(group.getId()),
                firstResult, maxResults);
    }

    /**
     * {@link UserQueryProvider}
     */
    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return kUsers.search(realm,
                new SearchCriteria().withSearch(attrValue),
                0, Integer.MAX_VALUE);
    }

    /**
     * {@link UserRegistrationProvider}
     */
    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return kUsers.addUser(realm, username);
    }

    /**
     * {@link UserRegistrationProvider}
     */
    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return kUsers.removeUser(realm, user);
    }
}
