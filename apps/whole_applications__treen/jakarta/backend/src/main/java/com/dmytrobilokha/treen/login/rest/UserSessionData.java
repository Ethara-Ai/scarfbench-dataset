package com.dmytrobilokha.treen.login.rest;

import jakarta.enterprise.context.SessionScoped;

import javax.annotation.CheckForNull;
import java.io.Serializable;

@SessionScoped
public class UserSessionData implements Serializable {

    @CheckForNull
    private volatile LoginData loginData;

    public void registerLogin(LoginData loginData) {
        this.loginData = loginData;
    }

    public void registerLogout() {
        this.loginData = null;
    }

    @CheckForNull
    public LoginData getLoginData() {
        return loginData;
    }

    public long getAuthenticatedUserId() {
        var userLoginData = loginData;
        if (userLoginData == null) {
            throw new IllegalStateException("Authorization filter should not allow non authenticated access");
        }
        return userLoginData.getUserId();
    }

}
