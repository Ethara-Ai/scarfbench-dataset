package com.dmytrobilokha.treen.infra.db;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@ApplicationScoped
public class DbUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbUpdater.class);

    private DataSource dataSource;

    DbUpdater() {
        //Constructor required to make DbUpdater proxable
    }

    @Inject
    public DbUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void update() {
        LOGGER.info("Initializing flyway DB migration");
        var flyway = Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db-script")
                .baselineVersion("0.0.0")
                .baselineOnMigrate(true)
                .failOnMissingLocations(true)
                .load();
        flyway.migrate();
    }

}
