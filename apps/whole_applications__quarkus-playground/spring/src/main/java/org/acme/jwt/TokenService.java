package org.acme.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  private final PrivateKey privateKey;

  private TokenService() throws Exception {
    this.privateKey = readPrivateKey("/private_key.pem");
  }

  /**
   * Returns a valid JWT with hardcoded claims (same claims as the former SmallRye JWT
   * implementation).
   *
   * @return a valid JWT
   * @throws Exception when parsing of private-key fails
   */
  public String generateTokenString() throws Exception {
    PrivateKey pk = readPrivateKey("/private_key.pem");
    return generateTokenString(pk, "/private_key.pem");
  }

  private String generateTokenString(PrivateKey privateKey, String kid) throws Exception {
    long currentTimeInSecs = currentTimeInSecs();
    long exp = currentTimeInSecs + 24 * 3600; // one day valid token
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("https://quarkus.io/using-jwt-rbac")
            .subject("jdoe-using-jwt-rbac")
            .claim("upn", "jdoe@quarkus.io")
            .claim("preferred_username", "jdoe")
            .audience("using-jwt-rbac")
            .claim("groups", List.copyOf(Set.of("Echoer", "Tester", "Subscriber", "group2")))
            .claim("birthday", "2001-07-13")
            .issueTime(new Date(currentTimeInSecs * 1000))
            .claim("auth_time", currentTimeInSecs)
            .expirationTime(new Date(exp * 1000))
            .build();
    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).type(com.nimbusds.jose.JOSEObjectType.JWT).build();
    SignedJWT jwt = new SignedJWT(header, claims);
    jwt.sign(new RSASSASigner(privateKey));
    return jwt.serialize();
  }

  private PrivateKey readPrivateKey(final String pemResName) throws Exception {
    try (InputStream contentIS = TokenService.class.getResourceAsStream(pemResName)) {
      byte[] tmp = new byte[4096];
      int length = contentIS.read(tmp);
      return decodePrivateKey(new String(tmp, 0, length, "UTF-8"));
    }
  }

  private PrivateKey decodePrivateKey(final String pemEncoded) throws Exception {
    byte[] encodedBytes = toEncodedBytes(pemEncoded);

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(keySpec);
  }

  private byte[] toEncodedBytes(final String pemEncoded) {
    final String normalizedPem = removeBeginEnd(pemEncoded);
    return Base64.getDecoder().decode(normalizedPem);
  }

  private String removeBeginEnd(String pem) {
    pem = pem.replaceAll("-----BEGIN (.*)-----", "");
    pem = pem.replaceAll("-----END (.*)----", "");
    pem = pem.replaceAll("\r\n", "");
    pem = pem.replaceAll("\n", "");
    return pem.trim();
  }

  private long currentTimeInSecs() {
    long currentTimeMS = System.currentTimeMillis();
    return currentTimeMS / 1000;
  }
}
