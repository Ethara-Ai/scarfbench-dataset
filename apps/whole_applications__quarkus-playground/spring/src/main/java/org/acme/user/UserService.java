package org.acme.user;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User save(User u) {
    userRepository.persist(u);
    return u;
  }

  @Transactional
  public User update(User u) {
    return userRepository.update(u);
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  @Transactional
  public boolean deleteById(Long id) {
    return userRepository.deleteById(id);
  }
}
