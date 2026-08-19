package com.dmytrobilokha.treen.login.persistence;

import com.dmytrobilokha.treen.infra.db.DbException;
import com.dmytrobilokha.treen.infra.db.DbQueryExecutor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.annotation.CheckForNull;

@ApplicationScoped
public class UserRepository {

    private DbQueryExecutor queryExecutor;

    public UserRepository() {
        //CDI bean
    }

    @Inject
    public UserRepository(DbQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @CheckForNull
    public UserEntity findUserByLogin(String login) throws DbException {
        return queryExecutor.selectObject(new FindUserByLoginQuery(login));
    }

    @CheckForNull
    public Long insertUser(String login, byte[] passwordHash, byte[] passwordSalt) throws DbException {
        return queryExecutor.insert(new InsertUserQuery(login, passwordHash, passwordSalt));
    }

    public int updateUserPassword(String login, byte[] passwordHash, byte[] passwordSalt) throws DbException {
        return queryExecutor.update(new UpdateUserPasswordQuery(login, passwordHash, passwordSalt));
    }

}
