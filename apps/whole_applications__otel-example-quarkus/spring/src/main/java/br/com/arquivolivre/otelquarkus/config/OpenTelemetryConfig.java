package br.com.arquivolivre.otelquarkus.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes an OpenTelemetry {@link Meter} bean so services can register custom metrics via
 * constructor injection (mirrors the Meter injection Quarkus provided out of the box).
 */
@Configuration
public class OpenTelemetryConfig {

    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("otel-spring-crud");
    }
}
