package org.acme;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/boom", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoomController {

  @GetMapping
  public ResponseEntity<Void> boom() {
    throw new RuntimeException("BOOM, request exploded");
  }
}
