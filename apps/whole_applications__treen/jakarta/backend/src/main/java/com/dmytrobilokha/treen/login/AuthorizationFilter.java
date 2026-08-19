package com.dmytrobilokha.treen.login;

import com.dmytrobilokha.treen.login.rest.UserSessionData;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class AuthorizationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final Set<String> SECURED_PATHS = Set.of("/service/");
    private static final Set<String> EXCLUSION_PATHS = Set.of("/service/auth/login");

    private UserSessionData userSessionData;

    public AuthorizationFilter() {
        //Framework
    }

    @Inject
    public AuthorizationFilter(UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (userSessionData.getLoginData() != null) {
            LOGGER.info("Login data is not null, filtering");
            chain.doFilter(request, response);
            return;
        }
        var httpRequest = (HttpServletRequest) request;
        var requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        var securedPath = SECURED_PATHS
                .stream()
                .anyMatch(requestPath::startsWith);
        var exclusionPath = EXCLUSION_PATHS
                .stream()
                .anyMatch(requestPath::startsWith);
        if (!securedPath || exclusionPath) {
            LOGGER.info("URI: {}. Filtering, securedPath={}, exclusionPath={}",
                    requestPath, securedPath, exclusionPath);
            chain.doFilter(request, response);
            return;
        }
        LOGGER.info("URI: {}. Blocking, securedPath={}, exclusionPath={}",
                requestPath, securedPath, exclusionPath);
        var httpResponse = (HttpServletResponse) response;
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must authenticate first");
    }

}
