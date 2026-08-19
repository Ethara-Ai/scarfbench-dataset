package com.dmytrobilokha.treen.login.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("user")
public class UserResource {

    private UserSessionData userSessionData;

    public UserResource() {
        //Framework
    }

    @Inject
    public UserResource(UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserDataResponse getUserData(@Context HttpServletRequest request) {
        var loginData = userSessionData.getLoginData();
        if (loginData == null) {
            throw new IllegalStateException(
                    "Authorization filter should not allow non-authenticated user to call this endpoint");
        }
        return new UserDataResponse(loginData.getLogin());
    }

}
