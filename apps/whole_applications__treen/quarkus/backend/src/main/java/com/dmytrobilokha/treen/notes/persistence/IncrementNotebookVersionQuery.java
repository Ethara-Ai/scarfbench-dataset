package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IncrementNotebookVersionQuery implements UpsertQuery {

    private static final String QUERY =
            "UPDATE user_data SET notebook_version=notebook_version+1 WHERE id=? AND notebook_version=?";

    private final long userId;
    private final long notebookVersion;

    IncrementNotebookVersionQuery(long userId, long notebookVersion) {
        this.userId = userId;
        this.notebookVersion = notebookVersion;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setLong(1, userId);
        statement.setLong(2, notebookVersion);
    }

}
