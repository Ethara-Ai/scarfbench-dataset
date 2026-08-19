package net.sourceforge.squirrel_sql.ws.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import net.sourceforge.squirrel_sql.ws.exceptions.AuthorizationException;
import net.sourceforge.squirrel_sql.ws.managers.TokensManager;

@Provider
@ApplicationScoped
public class AuthFilter implements ContainerRequestFilter {

    @Inject
    TokensManager tokensManager;

    Logger logger = Logger.getLogger(AuthFilter.class);

    @Override
    public void filter(ContainerRequestContext context) {

        // Migration note (jakarta -> quarkus): under RESTEasy, UriInfo.getPath()
        // may return the path with a leading slash (e.g. "/Authenticate") whereas
        // the original Jersey runtime returned it without one ("Authenticate").
        // Match the trailing path segment so the login endpoint stays unauthenticated.
        String path = context.getUriInfo().getPath();
        if (path == null) {
            path = "";
        }
        boolean isAuthenticateEndpoint = path.equals("Authenticate") || path.endsWith("/Authenticate");
        if (isAuthenticateEndpoint || tokensManager.isDebugMode()) {
            return;
        }

        String token;
        try {
            // Get the Authorization header from the request context
            token = tokensManager.extractTokenFromContext(context);
        } catch (AuthorizationException e) {
            throw unauthorized(e.getMessage());
        }

        try {
            // Check if token is valid
            tokensManager.validateToken(token);
        } catch (AuthorizationException e) {
            throw unauthorized(e.getMessage());
        }
    }

    /**
     * Migration note (jakarta -> quarkus): the former {@code @Context
     * HttpServletResponse} field is gone (no servlet container). We build the 401
     * response, including the {@code WWW-Authenticate} challenge header, directly
     * as a JAX-RS {@link Response}.
     */
    private WebApplicationException unauthorized(String message) {
        Response response = Response.status(Status.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, TokensManager.AUTHENTICATION_SCHEME)
                .entity(message)
                .build();
        return new WebApplicationException(message, response);
    }
}
