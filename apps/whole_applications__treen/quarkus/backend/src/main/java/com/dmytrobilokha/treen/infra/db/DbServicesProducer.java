package com.dmytrobilokha.treen.infra.db;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import javax.sql.DataSource;

@ApplicationScoped
public class DbServicesProducer {

    // The original application looked the DataSource up from JNDI (jdbc/TreenDB).
    // On Quarkus the DataSource is provided by the Agroal extension and configured
    // via application.properties (quarkus.datasource.*), so it is injected directly.
    private final DataSource dataSource;

    @Inject
    public DbServicesProducer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Dependent
    @Produces
    DbQueryExecutor produceDbQueryExecutor() {
        return new DbQueryExecutor(dataSource);
    }

}
