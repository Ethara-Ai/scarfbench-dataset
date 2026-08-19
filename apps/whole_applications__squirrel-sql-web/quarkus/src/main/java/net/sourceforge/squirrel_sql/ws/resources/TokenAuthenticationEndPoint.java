package net.sourceforge.squirrel_sql.ws.resources;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import net.sourceforge.squirrel_sql.dto.ValueBean;
import net.sourceforge.squirrel_sql.ws.exceptions.AuthorizationException;
import net.sourceforge.squirrel_sql.ws.managers.TokensManager;
import net.sourceforge.squirrel_sql.ws.managers.UsersManager;
import net.sourceforge.squirrel_sql.ws.model.User;

/**
 * Authentication endpoint for token-based (JWT) security.
 * 
 * @author lv 2017-2020
 */
@RequestScoped
@Path("/")
public class TokenAuthenticationEndPoint {

    Logger logger = Logger.getLogger(SessionsEndpoint.class);

    @Inject
    UsersManager usersManager;
    @Inject
    TokensManager tokensManager;

    /**
     * Plain-test authentication point.
     * 
     * @param username
     * @param password
     * @return
     */
    @POST
    @Path("Authenticate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String authenticate(@FormParam("username") String username, @FormParam("password") String password) {

        return internalAuthenticate(username, password);
    }

    /**
     * JSON authentication point.
     * 
     * @param credentials
     * @return
     */
    @POST
    @Path("Authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValueBean<String> authenticate(Credentials credentials) {

        String token = internalAuthenticate(credentials.getUsername(), credentials.getPassword());
        return new ValueBean<>(token);
    }

    /**
     * Return the user that the Authorization token was issued for.
     * 
     * @param request
     * @return
     * @throws AuthorizationException if token is invalid (this should not happen,
     *                                if filter is cconfigured properly)
     */
    @GET
    @Path("/CurrentUser")
    public ValueBean<User> getCurrentUser(@Context HttpHeaders headers) throws AuthorizationException {
        User user;
        if (!tokensManager.isDebugMode()) {
            String token = tokensManager.extractTokenFromHttpHeaders(headers);
            user = usersManager.findByUsername(tokensManager.getSubject(token));
        } else {
            user = new User();
            user.setUsername("admin");
            user.setName("John");
            user.setSurname("Doe");
            user.setEmail("johndoe@example.com");
            user.setRoles(new String[] { "admin" });
        }
        return new ValueBean<>(user);
    }

    /**
     * Common code
     * 
     * @param credentials
     * @return
     */
    private String internalAuthenticate(String username, String password) {

        if (username == null || username.isEmpty()) {
            throw unauthorized("Missing credentials");
        }

        User user = usersManager.findByUsernamePassword(username, password);

        if (user == null) {
            throw unauthorized("Invalid credentials");
        }

        // At last, user is authenticated
        logger.info("User authenticated: " + user);

        return tokensManager.issueToken(user);
    }

    /**
     * Build a 401 response carrying the {@code WWW-Authenticate} challenge header.
     *
     * Migration note (jakarta -> quarkus): replaces the former
     * {@code @Context HttpServletResponse response.setHeader(...)} calls, which
     * required a servlet container.
     */
    private WebApplicationException unauthorized(String message) {
        Response response = Response.status(Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, TokensManager.AUTHENTICATION_SCHEME)
                .entity(message)
                .build();
        return new WebApplicationException(message, response);
    }

    /**
     * This bean can be used from frontend for JSON authentication
     */
    public static class Credentials {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
