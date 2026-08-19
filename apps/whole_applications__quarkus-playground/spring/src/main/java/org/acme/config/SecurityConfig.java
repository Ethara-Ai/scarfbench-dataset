package org.acme.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Value("${acme.jwt.public-key-location}")
  private Resource publicKeyLocation;

  @Value("${acme.jwt.issuer}")
  private String issuer;

  /**
   * Security rules: /secured/roles-allowed requires the Echoer or Subscriber group (parity with
   * the former @RolesAllowed), everything else is public. JWT bearer tokens are validated with
   * the RSA public key.
   *
   * @param http the http security builder
   * @return the security filter chain
   * @throws Exception on configuration errors
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/secured/roles-allowed")
                    .hasAnyAuthority("Echoer", "Subscriber")
                    .anyRequest()
                    .permitAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /**
   * Builds a JwtDecoder from the local RSA public key and validates the issuer (parity with
   * mp.jwt.verify.publickey.location and mp.jwt.verify.issuer).
   *
   * @return the jwt decoder
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    try {
      String pem = new String(publicKeyLocation.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      String normalized =
          pem.replaceAll("-----BEGIN (.*)-----", "")
              .replaceAll("-----END (.*)-----", "")
              .replaceAll("\\s", "");
      byte[] encoded = Base64.getDecoder().decode(normalized);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
      NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
      decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
      return decoder;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read jwt public key", e);
    }
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("groups");
    authoritiesConverter.setAuthorityPrefix("");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    converter.setPrincipalClaimName("upn");
    return converter;
  }
}
