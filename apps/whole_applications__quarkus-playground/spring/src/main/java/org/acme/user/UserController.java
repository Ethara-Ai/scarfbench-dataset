package org.acme.user;

import jakarta.validation.Valid;
import org.acme.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Returns the user with the given id. Or NOT_FOUND.
   *
   * @param id id
   * @return the user
   */
  @GetMapping("/{id}")
  public User getUser(@PathVariable("id") Long id) {
    var userOptional = userService.findById(id);
    if (userOptional.isPresent()) {
      return userOptional.get();
    }
    throw new NotFoundException(String.format("Given user id %d does not exist.", id));
  }

  /**
   * Returns all users from the database.
   *
   * @return all users
   */
  @GetMapping
  public GetUsersResponse getUsers() {
    var users = userService.findAll();
    var response = new GetUsersResponse();
    response.setUsers(users);
    return response;
  }

  /**
   * Saves the given user data in the database.
   *
   * @param u the json request body
   * @return the stored user with database id
   */
  @PostMapping
  public ResponseEntity<User> createUser(@Valid @RequestBody User u) {
    var user = userService.save(u);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PutMapping("/{id}")
  public User updateUser(@Valid @RequestBody User u) {
    return userService.update(u);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
    userService.deleteById(id);
    return ResponseEntity.ok().build();
  }
}
