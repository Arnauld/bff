package org.technbolts.keycloak.users.jdbc;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.technbolts.keycloak.users.KUsersStorageProvider;
import org.technbolts.utils.DataSourceCache;

import java.time.Duration;
import java.util.List;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class JdbcKUserStorageProviderFactory implements UserStorageProviderFactory<KUsersStorageProvider> {
    private final Logger logger = Logger.getLogger(JdbcKUserStorageProviderFactory.class);

    private final List<ProviderConfigProperty> providerConfigProperties;
    private final DataSourceCache dataSourceCache;

    public JdbcKUserStorageProviderFactory() {
        this.providerConfigProperties = Config.newConfigMetadata();
        this.dataSourceCache = new DataSourceCache(Duration.ofMinutes(3)); // Assume the expiration duration is greater than the usage duration
    }

    @Override
    public String getId() {
        return "jdbc-user-provider";
    }

    @Override
    public KUsersStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        logger.infof("Creating new provider '%s'", ksession);
        return new KUsersStorageProvider(ksession, model, createUsers(ksession, model));
    }

    protected JdbcKUsers createUsers(KeycloakSession ksession, ComponentModel model) {
        return new JdbcKUsers(ksession, model,
                dataSourceCache.obtainDataSource(
                        model.get(Config.CONFIG_KEY_JDBC_URL),
                        model.get(Config.CONFIG_KEY_DB_USERNAME),
                        model.get(Config.CONFIG_KEY_DB_PASSWORD)));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return providerConfigProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession ksession, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        try (JdbcKUsers users = createUsers(ksession, config)) {
            users.checkConfig();
        } catch (Exception ex) {
            logger.warnf(ex, "Error while validating config");
            throw new ComponentValidationException("Unable to validate configuration", ex);
        }
    }

}
