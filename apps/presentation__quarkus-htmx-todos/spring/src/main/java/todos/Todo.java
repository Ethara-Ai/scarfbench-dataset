package todos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * The JPA entity, preserved as the semantic persistence model (checklist D2/E9).
 *
 * <p>Field names, column names, types, the UUID primary key and both accessors are the
 * source's. Three things are gone, and all three carried a source-framework runtime
 * assumption (D10):
 *
 * <ul>
 *   <li>{@code extends PanacheEntityBase} — the Panache active-record base class. Its
 *       finders live in {@link TodoRepository} now.
 *   <li>the {@code static} query methods — same reason.
 *   <li>{@code @org.jboss.resteasy.annotations.jaxrs.FormParam} on {@code title}, which
 *       made the entity double as a JAX-RS form-binding target. The controller reads the
 *       form field explicitly instead, so the entity is no longer part of the HTTP layer.
 * </ul>
 *
 * <p>The Jakarta Persistence and Hibernate annotations are unchanged — both frameworks
 * run the same ORM, so these are not framework-specific in the sense that matters here.
 */
@Entity
@Table(name = "todos")
public class Todo {

  @Id
  @GeneratedValue
  public UUID id;

  public String title;

  public Boolean completed = Boolean.FALSE;

  @CreationTimestamp
  @Column(name = "created_timestamp")
  public Timestamp createdTimestamp;

  public boolean isNotCompleted() {
    return !completed;
  }

  public Boolean getCompleted() {
    return completed;
  }
}
