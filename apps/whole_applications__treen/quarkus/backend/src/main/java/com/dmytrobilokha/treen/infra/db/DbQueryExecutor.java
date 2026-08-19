package com.dmytrobilokha.treen.infra.db;

import javax.annotation.CheckForNull;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DbQueryExecutor {

    private final DataSource dataSource;

    public DbQueryExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int update(UpsertQuery updateQuery) throws DbException {
        try (var connection = dataSource.getConnection();
            var statement = connection.prepareStatement(updateQuery.getQueryString())) {
            updateQuery.setParameters(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Failure while trying to execute update query", e);
        }
    }

    public int delete(UpsertQuery deleteQuery) throws DbException {
        return update(deleteQuery);
    }

    @CheckForNull
    public Long insert(UpsertQuery insertQuery) throws DbException {
        try (var connection = dataSource.getConnection();
            var statement = connection
                    .prepareStatement(insertQuery.getQueryString(), Statement.RETURN_GENERATED_KEYS)) {
            insertQuery.setParameters(statement);
            var insertedRows = statement.executeUpdate();
            if (insertedRows == 0) {
                return null;
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new DbException("Failure while trying to execute insert query", e);
        }
    }

    @CheckForNull
    public <T> T selectObject(SelectQuery<T> selectQuery) throws DbException {
        try (var connection = dataSource.getConnection();
            var statement = connection.prepareStatement(selectQuery.getQueryString())) {
            selectQuery.setParameters(statement);
            try (var resultSet = statement.executeQuery()) {
                return selectQuery.mapResultSet(resultSet);
            }
        } catch (SQLException e) {
            throw new DbException("Failure while trying to execute select object query", e);
        }
    }

    public <T> List<T> selectList(SelectQuery<T> selectQuery) throws DbException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(selectQuery.getQueryString())) {
            selectQuery.setParameters(statement);
            try (var resultSet = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(selectQuery.mapResultSet(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DbException("Failure while trying to execute select list query", e);
        }
    }

}
