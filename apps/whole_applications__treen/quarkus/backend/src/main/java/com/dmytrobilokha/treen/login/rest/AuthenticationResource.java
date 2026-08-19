package com.dmytrobilokha.treen.login.rest;

import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.infra.rest.ExceptionResponse;
import com.dmytrobilokha.treen.login.service.AuthenticationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Path("auth")
public class AuthenticationResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final int HALF_HOUR_SECONDS = 3600 / 2;

    private UserSessionData userSessionData;
    private AuthenticationService authenticationService;

    public AuthenticationResource() {
        //Framework requirement
    }

    @Inject
    public AuthenticationResource(UserSessionData userSessionData, AuthenticationService authenticationService) {
        this.userSessionData = userSessionData;
        this.authenticationService = authenticationService;
    }

    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(@Context HttpServletRequest request, @Valid LoginRequest loginRequest)
        throws InternalApplicationException {
        var login = loginRequest.getLogin();
        var password = loginRequest.getPassword();
        var userEntity = authenticationService.checkUserCredentials(login, password.toCharArray());
        if (userEntity != null) {
            invalidateRequestSession(request);
            var session = request.getSession(true);
            session.setMaxInactiveInterval(loginRequest.isRememberMe() ? -1 : HALF_HOUR_SECONDS);
            var loginData = new LoginData(userEntity.getId(), userEntity.getLogin());
            userSessionData.registerLogin(loginData);
            return Response.ok().build();
        }
        LOG.warn("Failed to login using {}", loginRequest);
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ExceptionResponse("Login/password pair is wrong"))
                .build();
    }

    @POST
    @Path("logout")
    public Response logout(@Context HttpServletRequest request) {
        userSessionData.registerLogout();
        invalidateRequestSession(request);
        return Response.ok().build();
    }

    private void invalidateRequestSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

}
