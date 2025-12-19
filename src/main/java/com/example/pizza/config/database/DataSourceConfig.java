package com.example.pizza.config.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        config.setMaximumPoolSize(3);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);     // 30 seconds
        config.setIdleTimeout(600000);          // 10 minutes
        config.setMaxLifetime(1800000);         // 30 minutes
        config.setLeakDetectionThreshold(30000); // (30 seconds)

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Pool naming
        config.setPoolName("PizzaAppHikariPool");

        // Additional PostgreSQL specific settings - Transaction mode için optimize edilmiş
        config.addDataSourceProperty("cachePrepStmts", "false");
        config.addDataSourceProperty("prepStmtCacheSize", "0");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "0");
        config.addDataSourceProperty("useServerPrepStmts", "false");
        config.addDataSourceProperty("reWriteBatchedInserts", "false");
        config.addDataSourceProperty("prepareThreshold", "0");
        config.addDataSourceProperty("disableColumnSanitiser", "false");

        // Connection retry settings
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "30");

        return new HikariDataSource(config);
    }
}