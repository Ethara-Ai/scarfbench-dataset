package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MoveNoteQuery implements UpsertQuery {

    private static final String QUERY = "UPDATE note n"
            + " INNER JOIN user_data ud ON ud.id=n.user_id"
            + " INNER JOIN note pn ON pn.user_id=n.user_id"
            + " SET n.parent_id=pn.id, ud.notebook_version=ud.notebook_version+1"
            + " WHERE n.id=? AND pn.id=? AND n.user_id=? AND ud.notebook_version=?";

    private final long noteId;
    private final long newParentId;
    private final long userId;
    private final long notebookVersion;

    MoveNoteQuery(long noteId, long newParentId, long userId, long notebookVersion) {
        this.noteId = noteId;
        this.newParentId = newParentId;
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
        statement.setLong(2, newParentId);
        statement.setLong(3, userId);
        statement.setLong(4, notebookVersion);
    }

}
