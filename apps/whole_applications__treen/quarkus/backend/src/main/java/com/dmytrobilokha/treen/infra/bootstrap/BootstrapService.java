package com.dmytrobilokha.treen.infra.bootstrap;

import com.dmytrobilokha.treen.infra.db.DbUpdater;
import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.login.service.AuthenticationService;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Application bootstrap.
 *
 * <p>The original application used a {@code jakarta.servlet.ServletContextListener}
 * ({@code @WebListener}) to run the Flyway DB migration and register the
 * authentication JMX bean on startup. On Quarkus the datasource runtime
 * configuration is not yet available while the Undertow servlet container is
 * booting its context listeners, so the same work is performed by observing the
 * Quarkus {@link StartupEvent}/{@link ShutdownEvent} lifecycle, which fires once
 * the datasource and the rest of the runtime configuration are ready.</p>
 */
@ApplicationScoped
public class BootstrapService {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapService.class);

    private static final String AUTH_SERVICE_JMX_NAME = "treen:type=AuthenticationService";

    private final DbUpdater dbUpdater;
    private final AuthenticationService authenticationService;

    // Optional demo-user seeding, disabled by default so production behaviour is
    // unchanged. Enable with -Dtreen.demo.seed-user.enabled=true or the env var
    // TREEN_DEMO_SEED_USER_ENABLED=true (used by the container smoke tests).
    private final boolean seedDemoUser;
    private final String demoUserLogin;
    private final String demoUserPassword;

    @Inject
    public BootstrapService(
            DbUpdater dbUpdater,
            AuthenticationService authenticationService,
            @ConfigProperty(name = "treen.demo.seed-user.enabled", defaultValue = "false") boolean seedDemoUser,
            @ConfigProperty(name = "treen.demo.seed-user.login", defaultValue = "demo") String demoUserLogin,
            @ConfigProperty(name = "treen.demo.seed-user.password", defaultValue = "demo1234") String demoUserPassword) {
        this.dbUpdater = dbUpdater;
        this.authenticationService = authenticationService;
        this.seedDemoUser = seedDemoUser;
        this.demoUserLogin = demoUserLogin;
        this.demoUserPassword = demoUserPassword;
    }

    void onStart(@Observes StartupEvent startupEvent) {
        dbUpdater.update();
        seedDemoUserIfEnabled();
        registerJmxBean();
    }

    private void seedDemoUserIfEnabled() {
        if (!seedDemoUser) {
            return;
        }
        try {
            authenticationService.createUser(demoUserLogin, demoUserPassword);
            LOG.info("Seeded demo user '{}' for smoke testing", demoUserLogin);
        } catch (InternalApplicationException e) {
            LOG.warn("Failed to seed demo user '{}' (it may already exist)", demoUserLogin, e);
        }
    }

    void onStop(@Observes ShutdownEvent shutdownEvent) {
        unregisterJmxBean();
    }

    private void registerJmxBean() {
        var server = ManagementFactory.getPlatformMBeanServer();
        try {
            var objectName = new ObjectName(AUTH_SERVICE_JMX_NAME);
            server.registerMBean(authenticationService, objectName);
        } catch (MBeanRegistrationException | MalformedObjectNameException
                | InstanceAlreadyExistsException | NotCompliantMBeanException e) {
            LOG.error("Failed to register authentication service JMX bean", e);
        }
    }

    private void unregisterJmxBean() {
        var server = ManagementFactory.getPlatformMBeanServer();
        try {
            var objectName = new ObjectName(AUTH_SERVICE_JMX_NAME);
            server.unregisterMBean(objectName);
        } catch (MBeanRegistrationException | MalformedObjectNameException | InstanceNotFoundException e) {
            LOG.error("Failed to unregister authentication service JMX bean", e);
        }
    }

}
