package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MoveNoteToRootQuery implements UpsertQuery {

    private static final String QUERY = "UPDATE note n"
            + " INNER JOIN user_data ud ON ud.id=n.user_id"
            + " SET n.parent_id=NULL, ud.notebook_version=ud.notebook_version+1"
            + " WHERE n.id=? AND n.user_id=? AND ud.notebook_version=?";

    private final long noteId;
    private final long userId;
    private final long notebookVersion;

    MoveNoteToRootQuery(long noteId, long userId, long notebookVersion) {
        this.noteId = noteId;
        this.userId = userId;
        this.notebookVersion = notebookVersion;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setLong(1, noteId);
        statement.setLong(2, userId);
        statement.setLong(3, notebookVersion);
    }

}
