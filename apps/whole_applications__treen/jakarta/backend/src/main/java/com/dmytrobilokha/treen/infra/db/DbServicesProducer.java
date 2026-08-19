package com.dmytrobilokha.treen.infra.db;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;

@ApplicationScoped
public class DbServicesProducer {

    @Resource(name = "jdbc/TreenDB")
    private DataSource dataSource;

    @Dependent
    @Produces
    DbQueryExecutor produceDbQueryExecutor() {
        return new DbQueryExecutor(dataSource);
    }

    @Dependent
    @Produces
    DataSource produceDbDataSource() {
        return dataSource;
    }

}
