package org.technbolts.keycloak.users.jdbc;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class Config {

    public static final String CONFIG_KEY_JDBC_DRIVER = "jdbcDriver";
    public static final String CONFIG_KEY_JDBC_URL = "jdbcUrl";
    public static final String CONFIG_KEY_DB_USERNAME = "dbUsername";
    public static final String CONFIG_KEY_DB_PASSWORD = "dbPassword";
    public static final String CONFIG_KEY_DB_VALIDATION_QUERY = "dbValidationQuery";

    public static List<ProviderConfigProperty> newConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(CONFIG_KEY_JDBC_DRIVER)
                .label("JDBC Driver Class")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("org.postgresql.Driver")
                .helpText("Fully qualified class name of the JDBC driver")
                .add()
                .property()
                .name(CONFIG_KEY_JDBC_URL)
                .label("JDBC URL")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue("jdbc:postgresql://localhost:5435/app_db")
                .helpText("JDBC URL used to connect to the user database")
                .add()
                .property()
                .name(CONFIG_KEY_DB_USERNAME)
                .label("Database User")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Username used to connect to the database")
                .add()
                .property()
                .name(CONFIG_KEY_DB_PASSWORD)
                .label("Database Password")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("Password used to connect to the database")
                .secret(true)
                .add()
                .property()
                .name(CONFIG_KEY_DB_VALIDATION_QUERY)
                .label("SQL Validation Query")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("SQL query used to validate a connection")
                .defaultValue("select 1")
                .add()
                .build();
    }
}
