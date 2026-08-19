package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertNoteQuery implements UpsertQuery {

    private static final String QUERY =
            "INSERT INTO note (parent_id, user_id, title, link, flags, description)"
                    + " SELECT pn.id, pn.user_id, ?, ?, ?, ?"
                    + " FROM note pn"
                    + " INNER JOIN user_data ud ON pn.user_id=ud.id"
                    + " WHERE pn.id=? AND pn.user_id=? AND ud.notebook_version=?";

    private final NewNote note;

    public InsertNoteQuery(NewNote note) {
        this.note = note;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, note.getTitle());
        statement.setString(2, note.getLink());
        statement.setLong(3, note.getFlags());
        statement.setString(4, note.getDescription());
        statement.setObject(5, note.getParentId());
        statement.setLong(6, note.getUserId());
        statement.setLong(7, note.getVersion());
    }

}
