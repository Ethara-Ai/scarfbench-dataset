package org.acme.middleware;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Ports the former JAX-RS MetricsRequestFilter/MetricsResponseFilter pair: counts http requests
 * and records request durations, tagged with method, handler (path template) and status code.
 */
@Component
public class MetricsFilter extends OncePerRequestFilter {

  private final MeterRegistry meterRegistry;

  public MetricsFilter(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      String handler = pattern != null ? pattern.toString() : "NOT_FOUND";
      String method = request.getMethod();
      String code = String.valueOf(response.getStatus());
      Counter.builder("http_requests")
          .description("number of http requests")
          .tag("method", method)
          .tag("handler", handler)
          .tag("code", code)
          .register(meterRegistry)
          .increment();
      DistributionSummary.builder("http_request_duration")
          .description("duration of a request")
          .baseUnit("milliseconds")
          .tag("method", method)
          .tag("handler", handler)
          .tag("code", code)
          .register(meterRegistry)
          .record(duration);
    }
  }
}
