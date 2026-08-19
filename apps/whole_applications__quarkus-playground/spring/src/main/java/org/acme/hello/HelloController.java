package org.acme.hello;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
public class HelloController {

  /**
   * Returns hello.
   *
   * @return hello
   */
  @GetMapping
  public HelloResponse hello() {
    return new HelloResponse("hello");
  }

  /**
   * Says a hello with the given message.
   *
   * @param param some json body
   * @return a json response
   */
  @PostMapping
  public ResponseEntity<HelloResponse> pushHello(
      @RequestBody(required = false) HelloRequest param) {
    var message = "";
    if (param != null && param.getMessage() != null) {
      message = param.getMessage();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(new HelloResponse(message));
  }
}
