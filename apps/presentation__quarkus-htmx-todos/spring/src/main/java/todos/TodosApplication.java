package todos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap class.
 *
 * <p>The source has no equivalent: Quarkus builds its container at augmentation time and
 * needs no application entry point. This is the "configuration / bootstrap classes are
 * replaced" decision (checklist D7) in its simplest form — nothing was preserved from the
 * source here because there was nothing to preserve.
 */
@SpringBootApplication
public class TodosApplication {

  public static void main(String[] args) {
    SpringApplication.run(TodosApplication.class, args);
  }
}
