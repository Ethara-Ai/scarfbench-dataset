package com.dmytrobilokha.treen.login.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertUserQuery implements UpsertQuery {

    private static final String QUERY =
            "INSERT INTO user_data (login, password_hash, password_salt, notebook_version) VALUES (?, ?, ?, 1)";

    private final String login;
    private final byte[] passwordHash;
    private final byte[] passwordSalt;

    InsertUserQuery(String login, byte[] passwordHash, byte[] passwordSalt) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, login);
        statement.setBytes(2, passwordHash);
        statement.setBytes(3, passwordSalt);
    }

}
