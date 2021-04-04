package org.technbolts.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class DataSourceCache {

    private static final Logger logger = Logger.getLogger(DataSourceCache.class);
    private final LoadingCache<Key, HikariDataSource> dataSources;

    public DataSourceCache(Duration expireAfterAccess) {
        dataSources = CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfterAccess)
                .removalListener((RemovalNotification<Key, HikariDataSource> removalNotification) -> {
                    closeSilently(removalNotification.getKey(), removalNotification.getValue());
                })
                .build(
                        new CacheLoader<Key, HikariDataSource>() {
                            public HikariDataSource load(Key key) { // no checked exception
                                return openDataSource(key);
                            }
                        });
    }

    public DataSource obtainDataSource(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
        Key key = new Key(jdbcUrl, jdbcUsername, jdbcPassword);
        try {
            return dataSources.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to obtain datasource " + key, e);
        }
    }

    private void closeSilently(Key key, HikariDataSource dataSource) {
        try {
            if (dataSource != null) {
                logger.infof("Releasing datasource %s", key);
                dataSource.close();
            }
        } catch (Exception e) {
            logger.warnf(e, "Error while closing datasource %s", key);
        }
    }

    protected HikariDataSource openDataSource(Key key) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(key.jdbcUrl);
        cfg.setUsername(key.jdbcUsername);
        cfg.setPassword(key.jdbcPassword);
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.setIdleTimeout(Duration.ofSeconds(25).toMillis());
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(0);
        return new HikariDataSource(cfg);
    }

    private static class Key {
        final String jdbcUrl;
        final String jdbcUsername;
        final String jdbcPassword;

        private Key(String jdbcUrl, String jdbcUsername, String jdbcPassword) {
            this.jdbcUrl = jdbcUrl;
            this.jdbcUsername = jdbcUsername;
            this.jdbcPassword = jdbcPassword;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Key key = (Key) o;
            if (!Objects.equals(jdbcUrl, key.jdbcUrl))
                return false;
            if (!Objects.equals(jdbcUsername, key.jdbcUsername))
                return false;
            return Objects.equals(jdbcPassword, key.jdbcPassword);
        }

        @Override
        public int hashCode() {
            int result = jdbcUrl != null ? jdbcUrl.hashCode() : 0;
            result = 31 * result + (jdbcUsername != null ? jdbcUsername.hashCode() : 0);
            result = 31 * result + (jdbcPassword != null ? jdbcPassword.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Key{" + jdbcUrl + ", " + jdbcUsername + '}';
        }
    }
}
