package org.technbolts.keycloak.users;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class JdbcUserStorageProviderFactory implements UserStorageProviderFactory<UsersStorageProvider> {
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
    public UsersStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        return new UsersStorageProvider(ksession, model, createUsers(ksession, model));
    }

    protected JdbcUsers createUsers(KeycloakSession ksession, ComponentModel model) {
        return new JdbcUsers(ksession, model, );
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return providerConfigProperties;
    }

    @Override
    public void validateConfiguration(KeycloakSession ksession, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        try (JdbcUsers users = createUsers(ksession, config)) {
            users.checkConfig();
        } catch (Exception ex) {
            logger.warnf(ex, "Error while validating config");
            throw new ComponentValidationException("Unable to validate configuration", ex);
        }
    }

    protected ConnectionProvider initDataSource(ComponentModel config) {
        return new ConnectionProvider() {
            @Override
            public Connection openConnection() {
                return null;
            }

            @Override
            public void dispose() {

            }
        }
    }

    protected AgroalDataSource openDataSource(ComponentModel config) throws SQLException {
        AgroalConnectionPoolConfigurationSupplier poolCfgSupplier =
                new AgroalConnectionPoolConfigurationSupplier()
                        .idleValidationTimeout(Duration.ofSeconds(15))
                        .maxSize(3)
                        .minSize(0);

        poolCfgSupplier.connectionFactoryConfiguration()
                .jdbcUrl(config.get(Config.CONFIG_KEY_JDBC_URL))
                .principal(new NamePrincipal(config.get(Config.CONFIG_KEY_DB_USERNAME)))
                .credential(new SimplePassword(config.get(Config.CONFIG_KEY_DB_PASSWORD)));

        return AgroalDataSource.from(
                new AgroalDataSourceConfigurationSupplier()
                        .connectionPoolConfiguration(poolCfgSupplier));
    }
}
