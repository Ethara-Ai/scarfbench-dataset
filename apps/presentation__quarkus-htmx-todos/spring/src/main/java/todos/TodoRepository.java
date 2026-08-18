package todos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The source's Panache active-record statics, re-expressed as target-native persistence
 * access (checklist E10).
 *
 * <p>Each method below answers exactly one {@code static} method that used to live on the
 * entity, and the mapping is one-for-one on purpose — the query semantics are the
 * contract, not the mechanism:
 *
 * <pre>
 *   Todo.listAll()                  -> findAllByOrderByCreatedTimestampAsc()
 *     findAll(Sort.ascending("createdTimestamp")).list()
 *   Todo.listActive()               -> findByCompletedFalseOrderByCreatedTimestampAsc()
 *     list("completed=false", Sort.ascending("createdTimestamp"))
 *   Todo.listCompleted()            -> findByCompletedTrueOrderByCreatedTimestampAsc()
 *     list("completed=true", Sort.ascending("createdTimestamp"))
 *   Todo.countActive()              -> countByCompletedFalse()
 *     count("completed != true")
 *   Todo.updateAllCompleted(flag)   -> updateAllCompleted(flag)
 *     update("completed = ?1", completed)          — bulk, every row
 *   Todo.deleteCompleted()          -> deleteCompleted()
 *     delete("completed = true")                   — bulk
 *   Todo.persist / findById / deleteById -> inherited from JpaRepository
 * </pre>
 *
 * <p>The two bulk operations stay bulk. Loading every row and mutating it one at a time
 * would produce the same rows and a different number of statements; "toggle all" on a
 * large list is a single UPDATE in the source and is a single UPDATE here.
 */
public interface TodoRepository extends JpaRepository<Todo, UUID> {

  List<Todo> findAllByOrderByCreatedTimestampAsc();

  List<Todo> findByCompletedFalseOrderByCreatedTimestampAsc();

  List<Todo> findByCompletedTrueOrderByCreatedTimestampAsc();

  long countByCompletedFalse();

  @Modifying
  @Query("update Todo t set t.completed = :completed")
  void updateAllCompleted(@Param("completed") boolean completed);

  @Modifying
  @Query("delete from Todo t where t.completed = true")
  void deleteCompleted();
}
