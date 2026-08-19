package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteNoteQuery implements UpsertQuery {

    private static final String QUERY =
            "DELETE n FROM note n"
                    + " INNER JOIN user_data ud ON n.user_id=ud.id"
                    + " WHERE n.id=? AND n.user_id=? AND ud.notebook_version=?";

    private final long id;
    private final long userId;
    private final long version;

    DeleteNoteQuery(long id, long userId, long version) {
        this.id = id;
        this.userId = userId;
        this.version = version;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setLong(1, id);
        statement.setLong(2, userId);
        statement.setLong(3, version);
    }

}
