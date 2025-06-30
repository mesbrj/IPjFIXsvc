package com.ipfix_scenario_ai.ipjfix_svc.config;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ipfix_scenario_ai.ipjfix_svc.core.models.User;

import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "ignite.enabled", havingValue = "true", matchIfMissing = false)
public class IgniteConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(IgniteConfig.class);
    
    @Value("${ignite.instance.name:ipfix-ignite}")
    private String instanceName;
    
    @Value("${ignite.work.directory:./ignite-work}")
    private String workDirectory;
    
    private Ignite ignite;

    @Bean
    public Ignite igniteInstance() {
        logger.info("Initializing Apache Ignite instance: {}", instanceName);
        
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(instanceName);
        cfg.setWorkDirectory(workDirectory);
        
        // Configure peer class loading for development
        cfg.setPeerClassLoadingEnabled(true);
        
        // Set cache configurations
        cfg.setCacheConfiguration(userCacheConfiguration());
        
        // Start Ignite
        this.ignite = Ignition.start(cfg);
        
        logger.info("Apache Ignite started successfully");
        return this.ignite;
    }
    
    private CacheConfiguration<String, User> userCacheConfiguration() {
        CacheConfiguration<String, User> cfg = new CacheConfiguration<>();
        cfg.setName("UserCache");
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        cfg.setBackups(1);
        // Temporarily disable indexing to avoid H2 compatibility issues
        // cfg.setIndexedTypes(String.class, User.class);
        
        // Enable statistics for monitoring
        cfg.setStatisticsEnabled(true);
        
        logger.debug("User cache configuration created");
        return cfg;
    }
    
    @PreDestroy
    public void cleanup() {
        if (ignite != null && !ignite.cluster().state().active()) {
            logger.info("Shutting down Ignite instance");
            ignite.close();
        }
    }
}
