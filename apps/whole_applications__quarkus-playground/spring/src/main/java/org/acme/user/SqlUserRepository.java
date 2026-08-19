package org.acme.user;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SqlUserRepository implements UserRepository {

  private final JpaUserRepository repo;

  public SqlUserRepository(JpaUserRepository repo) {
    this.repo = repo;
  }

  @Override
  public void persist(User user) {
    repo.save(user);
  }

  @Override
  public User update(User u) {
    return repo.save(u);
  }

  @Override
  public List<User> findAll() {
    return repo.findAll();
  }

  @Override
  public Optional<User> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean deleteById(Long id) {
    if (!repo.existsById(id)) {
      return false;
    }
    repo.deleteById(id);
    return true;
  }

  @Override
  public long deleteAll() {
    long count = repo.count();
    repo.deleteAll();
    return count;
  }
}
