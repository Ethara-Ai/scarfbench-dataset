package com.dmytrobilokha.treen.login.persistence;

public class UserEntity {

    private final long id;
    private final String login;
    private final byte[] passwordHash;
    private final byte[] passwordSalt;

    public UserEntity(long id, String login, byte[] passwordHash, byte[] passwordSalt) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }

    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    @Override
    public String toString() {
        return "UserEntity{"
                + "id=" + id
                + ", login='" + login + '\''
                + '}';
    }

}
