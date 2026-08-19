package com.dmytrobilokha.treen.login.persistence;

import com.dmytrobilokha.treen.infra.db.SelectQuery;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FindUserByLoginQuery implements SelectQuery<UserEntity> {

    private static final String QUERY = "SELECT id, login, password_hash, password_salt FROM user_data WHERE login=?";

    private final String login;

    FindUserByLoginQuery(String login) {
        this.login = login;
    }

    @Override
    public String getQueryString() {
        return QUERY;
    }

    @Override
    public void setParameters(PreparedStatement statement) throws SQLException {
        statement.setString(1, login);
    }

    @CheckForNull
    @Override
    public UserEntity mapResultSet(ResultSet resultSet) throws SQLException {
        if (!resultSet.next()) {
            return null;
        }
        byte[] userPasswordHash;
        byte[] userPasswordSalt;
        try {
            userPasswordHash = resultSet.getBinaryStream("password_hash").readAllBytes();
            userPasswordSalt = resultSet.getBinaryStream("password_salt").readAllBytes();
        } catch (IOException e) {
            throw new SQLException("Failed to read binary stream from the DB", e);
        }
        var userId = resultSet.getLong("id");
        var userLogin = resultSet.getString("login");
        return new UserEntity(userId, userLogin, userPasswordHash, userPasswordSalt);
    }

}
