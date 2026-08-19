package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application root. On Quarkus (RESTEasy Classic) an empty
 * {@link Application} subclass annotated with {@link ApplicationPath} simply
 * sets the REST base path; all {@code @Path}/{@code @Provider} classes are
 * still discovered automatically by classpath scanning.
 *
 * The former {@code @ApplicationScoped} annotation was removed: a JAX-RS
 * {@code Application} must not also be a CDI bean under Quarkus.
 */
@ApplicationPath("/ws")
public class JaxRsApplication extends Application {

}
