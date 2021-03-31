package org.technbolts.keycloak.users;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    private final Logger logger = Logger.getLogger(CustomUserStorageProviderFactory.class);
    private final List<ProviderConfigProperty> providerConfigProperties;

    public CustomUserStorageProviderFactory() {
        providerConfigProperties = Config.newConfigMetadata();
    }

    @Override
    public String getId() {
        return "custom-user-provider";
    }

    @Override
    public CustomUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        return new CustomUserStorageProvider(ksession, model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return providerConfigProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        try {
            JdbcUsers users = new JdbcUsers(config);
            users.checkConfig();
        } catch (Exception ex) {
            logger.warnf(ex, "Error while validating config");
            throw new ComponentValidationException("Unable to validate configuration", ex);
        }
    }
}
