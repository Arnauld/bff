package org.technbolts.keycloak.users.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.technbolts.keycloak.users.KUsersStorageProvider;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class JdbcKUserStorageProviderFactory implements UserStorageProviderFactory<KUsersStorageProvider> {
    private final Logger logger = Logger.getLogger(JdbcKUserStorageProviderFactory.class);
    private final List<ProviderConfigProperty> providerConfigProperties;

    public JdbcKUserStorageProviderFactory() {
        this.providerConfigProperties = Config.newConfigMetadata();
    }

    @Override
    public String getId() {
        return "jdbc-user-provider";
    }

    @Override
    public KUsersStorageProvider create(KeycloakSession ksession, ComponentModel model) {
        return new KUsersStorageProvider(ksession, model, createUsers(ksession, model));

    }

    protected JdbcKUsers createUsers(KeycloakSession ksession, ComponentModel model) {
        return new JdbcKUsers(ksession, model, initDataSource(model));
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

    private HikariDataSource dataSource;
    private String dataSourceKey;

    protected synchronized DataSource initDataSource(ComponentModel config) {
        String requiredKey = dataSourceKey(config);
        if (dataSourceKey == null || !dataSourceKey.equalsIgnoreCase(requiredKey)) {
            closeSilently(dataSource);
            dataSource = null;
            dataSourceKey = null;
            try {
                dataSource = openDataSource(config);
                dataSourceKey = requiredKey;
            } catch (SQLException e) {
                logger.errorf(e, "Failed to open datasource");
                throw new RuntimeException(e);
            }
        }
        return dataSource;
    }

    private void closeSilently(HikariDataSource dataSource) {
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            logger.warnf(e, "Error while closing datasource");
        }
    }

    private final String dataSourceKey(ComponentModel config) {
        return config.get(Config.CONFIG_KEY_JDBC_URL)
                + "::" + config.get(Config.CONFIG_KEY_DB_USERNAME)
                + "::" + config.get(Config.CONFIG_KEY_DB_PASSWORD);
    }

    protected HikariDataSource openDataSource(ComponentModel config) throws SQLException {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(config.get(Config.CONFIG_KEY_JDBC_URL));
        cfg.setUsername(config.get(Config.CONFIG_KEY_DB_USERNAME));
        cfg.setPassword(config.get(Config.CONFIG_KEY_DB_PASSWORD));
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.setIdleTimeout(Duration.ofSeconds(25).toMillis());
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(0);
        return new HikariDataSource(cfg);
    }
}
