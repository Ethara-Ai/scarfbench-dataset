package com.mongodb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The source variant keeps its classes in the {@code com.mongodb} package; the package
 * layout is preserved so the migration changes framework wiring only.
 */
@SpringBootApplication
public class MongodbCrudApplication {

    public static void main(String[] args) {
        SpringApplication.run(MongodbCrudApplication.class, args);
    }
}
