package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.SelectQuery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class FetchNotesQuery implements SelectQuery<Note> {

    private static final String QUERY = "SELECT n.id"
            + ", n.parent_id, n.user_id, n.title, n.link, n.flags, n.description"
            + ", ud.notebook_version version"
            + " FROM note n INNER JOIN user_data ud ON n.user_id=ud.id WHERE n.user_id=?";

    private final long notesOwnerId;

    FetchNotesQuery(long notesOwnerId) {
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

    @Override
    public Note mapResultSet(ResultSet resultSet) throws SQLException {
        var id = resultSet.getLong("id");
        var parentId = resultSet.getObject("parent_id", Long.class);
        var userId = resultSet.getLong("user_id");
        var title = resultSet.getString("title");
        var link = resultSet.getString("link");
        var flags = resultSet.getLong("flags");
        var description = resultSet.getString("description");
        var version = resultSet.getLong("version");
        return new Note(id, parentId, userId, title, link, flags, description, version);
    }

}
