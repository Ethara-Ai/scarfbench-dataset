package com.dmytrobilokha.treen.login.rest;

import jakarta.validation.constraints.Size;

public class LoginRequest {


    @Size(min = 1, max = 120, message = "Login should be between 1 and 120 characters long")
    private String login;
    @Size(min = 1, max = 120, message = "Password should be between 1 and 120 characters long")
    private String password;
    private boolean rememberMe;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @Override
    public String toString() {
        return "LoginRequest{"
                + "login='" + login + '\''
                + ", rememberMe=" + rememberMe
                + '}';
    }

}
