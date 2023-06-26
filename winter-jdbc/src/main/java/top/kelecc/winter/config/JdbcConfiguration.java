package top.kelecc.winter.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import top.kelecc.winter.annotation.Autowired;
import top.kelecc.winter.annotation.Bean;
import top.kelecc.winter.annotation.Configuration;
import top.kelecc.winter.annotation.Value;
import top.kelecc.winter.context.ApplicationContextUtils;
import top.kelecc.winter.jdbc.JdbcTemplate;
import top.kelecc.winter.jdbc.tx.DataSourceTransactionManager;
import top.kelecc.winter.jdbc.tx.PlatformTransactionManager;
import top.kelecc.winter.jdbc.tx.TransactionalBeanPostProcessor;

import javax.sql.DataSource;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/23 21:52
 */
@Configuration
public class JdbcConfiguration {
    @Bean(destroyMethod = "dataSourceClose")
    DataSource dataSource(
            @Value("${winter.datasource.url}") String url,
            @Value("${winter.datasource.username}") String username,
            @Value("${winter.datasource.password}") String password,
            @Value("${winter.datasource.driver-class-name}") String driver,
            @Value("${winter.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${winter.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${winter.datasource.connection-timeout:30000}") int connTimeout
    ) {
        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMaximumPoolSize(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }
    @Bean
    public PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    public void dataSourceClose() {
        HikariDataSource hikariDataSource = (HikariDataSource) ApplicationContextUtils.getRequiredApplicationContext().getBean("dataSource");
        hikariDataSource.close();
    }
}
