package com.dmytrobilokha.treen.login.service;

import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;

public interface AuthenticationServiceMXBean {

    void createUser(String login, String password) throws InternalApplicationException;

    int changeUserPassword(String login, String password) throws InternalApplicationException;
}
