package org.acme;

import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  /**
   * Application entry point. Sets the default timezone to UTC (parity with the former Quarkus
   * AppLifecycleBean).
   *
   * @param args cli args
   */
  public static void main(String[] args) {
    log.info("Starting application, set timezone to UTC");
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(Application.class, args);
  }
}
