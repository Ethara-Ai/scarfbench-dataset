package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.SelectQuery;

import javax.annotation.CheckForNull;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class FetchNotebookVersionQuery implements SelectQuery<Long> {

    private static final String QUERY = "SELECT notebook_version FROM user_data WHERE id=?";

    private final long notesOwnerId;

    FetchNotebookVersionQuery(long notesOwnerId) {
        this.notesOwnerId = notesOwnerId;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setLong(1, notesOwnerId);
    }

    @CheckForNull
    @Override
    public Long mapResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return resultSet.getLong("notebook_version");
        }
        return null;
    }

}
