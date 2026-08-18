package todos;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * {@code AppResource} rewritten to target conventions (checklist D5/E5).
 *
 * <p>Route unchanged: {@code GET /} answers <b>307 Temporary Redirect</b> to {@code /todos}.
 * Not 302, and not Spring's {@code "redirect:"} view prefix — that would send 302 and
 * change an observable status code. The status is stated explicitly because status control
 * matters here (E7).
 */
@Controller
public class AppController {

  @GetMapping("/")
  public ResponseEntity<Void> home() {
    return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
        .header(HttpHeaders.LOCATION, "/todos")
        .build();
  }
}
