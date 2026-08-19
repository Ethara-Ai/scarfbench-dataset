package com.dmytrobilokha.treen.login.persistence;

import com.dmytrobilokha.treen.infra.db.UpsertQuery;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateUserPasswordQuery implements UpsertQuery {

    private static final String QUERY =
            "UPDATE user_data SET password_hash=?, password_salt=? WHERE login=?";

    private final String login;
    private final byte[] passwordHash;
    private final byte[] passwordSalt;

    UpdateUserPasswordQuery(String login, byte[] passwordHash, byte[] passwordSalt) {
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
        statement.setBytes(1, passwordHash);
        statement.setBytes(2, passwordSalt);
        statement.setString(3, login);
    }

}
