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
public class JdbcUserStorageProviderFactory implements UserStorageProviderFactory<JdbcUserStorageProvider> {
    private final Logger logger = Logger.getLogger(JdbcUserStorageProviderFactory.class);
    private final List<ProviderConfigProperty> providerConfigProperties;

    public JdbcUserStorageProviderFactory() {
        this.providerConfigProperties = Config.newConfigMetadata();
    }

    @Override
    public String getId() {
        return "jdbc-user-provider";
    }

    @Override
    public JdbcUserStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        return new JdbcUserStorageProvider(ksession, model, createUsers(model));
    }

    protected JdbcUsers createUsers(ComponentModel model) {
        return new JdbcUsers(model);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return providerConfigProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        try (JdbcUsers users = createUsers(config)) {
            users.checkConfig();
        } catch (Exception ex) {
            logger.warnf(ex, "Error while validating config");
            throw new ComponentValidationException("Unable to validate configuration", ex);
        }
    }
}
