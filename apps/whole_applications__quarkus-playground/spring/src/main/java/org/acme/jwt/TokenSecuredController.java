package org.acme.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secured")
public class TokenSecuredController {

  private final TokenService tokenService;

  public TokenSecuredController(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  /**
   * Returns a valid JWT with hardcoded claims.
   *
   * @return a JWT
   * @throws Exception when parsing of private key fails
   */
  @GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
  public TokenReponse getToken() throws Exception {
    String token = this.tokenService.generateTokenString();
    TokenReponse response = new TokenReponse();
    response.setAccessToken(token);
    return response;
  }

  /**
   * Returns infos about the SecurityContext. Everyone is authorized.
   *
   * @param request the http request
   * @param authentication the current authentication
   * @return securityContext infos
   */
  @GetMapping(value = "/permit-all", produces = MediaType.TEXT_PLAIN_VALUE)
  public String hello(HttpServletRequest request, Authentication authentication) {
    return securityInfo(request, authentication);
  }

  /**
   * Returns infos about the SecurityContext. Only authenticated users with valid JWTs and role
   * Echoer or Subscriber are allowed (enforced in SecurityConfig).
   *
   * @param request the http request
   * @param authentication the current authentication
   * @return securityContext infos
   */
  @GetMapping(value = "/roles-allowed", produces = MediaType.TEXT_PLAIN_VALUE)
  public String helloRolesAllowed(HttpServletRequest request, Authentication authentication) {
    return securityInfo(request, authentication);
  }

  private String securityInfo(HttpServletRequest request, Authentication authentication) {
    boolean hasJwt = authentication instanceof JwtAuthenticationToken;
    boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;
    String name = anonymous ? "anonymous" : authentication.getName();
    String authScheme = hasJwt ? "Bearer" : null;
    return String.format(
        "hello + %s, isSecure: %s, authScheme: %s, hasJwt: %s",
        name, request.isSecure(), authScheme, hasJwt);
  }
}
