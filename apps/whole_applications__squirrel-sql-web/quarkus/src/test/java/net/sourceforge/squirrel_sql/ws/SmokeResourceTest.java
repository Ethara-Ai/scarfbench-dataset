package net.sourceforge.squirrel_sql.ws;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * End-to-end smoke tests for the migrated Quarkus application.
 *
 * These boot the whole Quarkus stack (including the eager SQuirreL
 * {@code WebApplication} startup and the JWT {@code AuthFilter}) and exercise the
 * REST surface over HTTP, mirroring what the original JavaEE/JAX-RS WAR exposed
 * under {@code /ws}.
 */
@QuarkusTest
public class SmokeResourceTest {

    /**
     * The JAX-RS layer is up: an unauthenticated protected endpoint must be
     * rejected by the AuthFilter with HTTP 401 (not 404), proving both routing
     * and the security filter work.
     */
    @Test
    public void helloWorldRequiresAuthentication() {
        given()
            .when().get("/ws/HelloWorld")
            .then().statusCode(401);
    }

    /**
     * The default admin/admin credentials (seeded into Users.xml on startup)
     * authenticate and yield a non-empty JWT.
     */
    @Test
    public void authenticateWithFormCredentialsReturnsToken() {
        String token = given()
                .contentType(ContentType.URLENC)
                .formParam("username", "admin")
                .formParam("password", "admin")
            .when().post("/ws/Authenticate")
            .then().statusCode(200)
                .extract().asString();

        assertNotNull(token);
        assertFalse(token.trim().isEmpty(), "Expected a non-empty JWT token");
    }

    /**
     * The JSON authentication variant returns a {@code ValueBean<String>} wrapping
     * the token.
     */
    @Test
    public void authenticateWithJsonCredentialsReturnsToken() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"username\":\"admin\",\"password\":\"admin\"}")
            .when().post("/ws/Authenticate")
            .then().statusCode(200)
                .body("value", notNullValue());
    }

    /**
     * Wrong credentials are rejected with HTTP 401.
     */
    @Test
    public void authenticateWithBadCredentialsIsRejected() {
        given()
                .contentType(ContentType.URLENC)
                .formParam("username", "admin")
                .formParam("password", "wrong")
            .when().post("/ws/Authenticate")
            .then().statusCode(401);
    }

    /**
     * A valid token grants access to a protected endpoint and the business
     * response is returned verbatim.
     */
    @Test
    public void validTokenGrantsAccessToProtectedEndpoint() {
        String token = given()
                .contentType(ContentType.URLENC)
                .formParam("username", "admin")
                .formParam("password", "admin")
            .when().post("/ws/Authenticate")
            .then().statusCode(200)
                .extract().asString();

        given()
                .header("Authorization", "Bearer " + token)
            .when().get("/ws/HelloWorld")
            .then().statusCode(200)
                .body(equalTo("Hello World"));
    }

    /**
     * The authenticated user can be resolved from the token.
     */
    @Test
    public void currentUserIsResolvedFromToken() {
        String token = given()
                .contentType(ContentType.URLENC)
                .formParam("username", "admin")
                .formParam("password", "admin")
            .when().post("/ws/Authenticate")
            .then().statusCode(200)
                .extract().asString();

        given()
                .header("Authorization", "Bearer " + token)
            .when().get("/ws/CurrentUser")
            .then().statusCode(200)
                .body("value.username", equalTo("admin"));
    }
}
