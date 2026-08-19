package org.acme.exception;

import jakarta.validation.ConstraintViolationException;
import org.acme.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception mappers. Ports the former JAX-RS ExceptionMapper providers
 * (ValidationExceptionHandler, NotFoundExceptionMapper, FallbackExceptionHandler) to a Spring
 * RestControllerAdvice.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Maps bean-validation failures of json request bodies to 400 with an ErrorResponse body.
   *
   * @param e the validation exception
   * @return 400 with error body
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    var message =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> String.format("%s: %s", fe.getField(), fe.getDefaultMessage()))
            .sorted()
            .reduce((a, b) -> a + ", " + b)
            .orElse("invalid request body");
    var error = new ErrorResponse();
    error.setCode("INVALID_REQUEST_BODY");
    error.setMessage(message);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Maps constraint violations to 400 with an ErrorResponse body.
   *
   * @param e the validation exception
   * @return 400 with error body
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    var error = new ErrorResponse();
    error.setCode("INVALID_REQUEST_BODY");
    error.setMessage(e.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  /**
   * Maps NotFoundException to an empty 404 response (parity with the former Quarkus
   * NotFoundExceptionMapper which returned no body).
   *
   * @param e the not-found exception
   * @return empty 404
   */
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Void> handleNotFound(NotFoundException e) {
    log.error("Not found error", e);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  /**
   * Maps unknown routes to an empty 404 response.
   *
   * @param e the no-resource exception
   * @return empty 404
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
  }

  /**
   * Fallback mapper: any unhandled exception becomes 500 with an ErrorResponse body.
   *
   * @param e the exception
   * @return 500 with error body
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleFallback(Exception e) {
    var error = new ErrorResponse();
    error.setCode("INTERNAL_SERVER_ERROR");
    error.setMessage("Internal server error");
    if (e != null) {
      error.setMessage(e.getMessage());
    }
    log.error("Internal Server error", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
