package todos;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code TodoResource} rewritten to target conventions (checklist D5/E5–E8).
 *
 * <p>All eight routes keep their paths and verbs — the HTTP surface is the contract:
 *
 * <pre>
 *   GET  /todos                    full list
 *   GET  /todos/active             active only
 *   GET  /todos/completed          completed only
 *   POST /todos                    add        — htmx: the new &lt;li&gt;, otherwise 302
 *   POST /todos/toggle-all         toggle every item
 *   POST /todos/{id}               rename
 *   POST /todos/{id}/toggle        toggle one — htmx: the updated &lt;li&gt;, otherwise 302
 *   POST /todos/{id}/delete        delete
 *   POST /todos/clear-completed    delete completed
 * </pre>
 *
 * <p>Three transformations are worth naming.
 *
 * <p><b>The htmx branch.</b> Two routes answer either a full redirect or a bare {@code <li>}
 * fragment depending on the {@code HX-Request} header, and {@code POST /todos} additionally
 * sets {@code HX-Trigger: clear-add-todo} on the fragment response. The source expressed
 * this with a {@code @HeaderParam boolean} and a return type of {@code Object}; here it is
 * a {@code @RequestHeader} and two different view names. The behaviour — including which
 * response carries which header — is identical.
 *
 * <p><b>Form binding.</b> The source bound the form onto the entity itself
 * ({@code @Form Todo todo}, with {@code @FormParam} on the field). That made the entity part
 * of the HTTP layer, which is exactly the kind of source-framework assumption not to carry
 * over, so the single submitted field is read explicitly as {@code @RequestParam}.
 *
 * <p><b>Literal versus templated paths.</b> {@code /todos/toggle-all} and
 * {@code /todos/clear-completed} would both also match {@code /todos/{id}}. JAX-RS resolves
 * this by preferring the literal path; Spring's {@code PathPattern} comparator does the
 * same, so no route needed reordering or a regex guard.
 */
@Controller
@RequestMapping("/todos")
public class TodoController {

  private final TodoRepository todos;

  TodoController(TodoRepository todos) {
    this.todos = todos;
  }

  @GetMapping
  public String list(Model model) {
    return showList(model, todos.findAllByOrderByCreatedTimestampAsc(), "all");
  }

  @GetMapping("/active")
  public String active(Model model) {
    return showList(model, todos.findByCompletedFalseOrderByCreatedTimestampAsc(), "active");
  }

  @GetMapping("/completed")
  public String completed(Model model) {
    return showList(model, todos.findByCompletedTrueOrderByCreatedTimestampAsc(), "completed");
  }

  private String showList(Model model, List<Todo> list, String filter) {
    model.addAttribute("todos", list);
    model.addAttribute("itemsLeft", (int) todos.countByCompletedFalse());
    // The source sets exactly one of these and leaves the others null, which its template
    // engine reads as false. Setting all three explicitly renders the same markup without
    // depending on how a missing value coerces.
    model.addAttribute("all", "all".equals(filter));
    model.addAttribute("active", "active".equals(filter));
    model.addAttribute("completed", "completed".equals(filter));
    return "todos/list";
  }

  @PostMapping
  @Transactional
  public String add(
      @RequestParam("title") String title,
      @RequestHeader(value = "HX-Request", defaultValue = "false") boolean hxRequest,
      Model model,
      HttpServletResponse response) {
    Todo todo = new Todo();
    todo.title = title;
    // saveAndFlush, not save: the fragment response renders todo.id and the generated
    // creation timestamp, so the INSERT has to have happened before the view runs.
    todos.saveAndFlush(todo);
    if (hxRequest) {
      model.addAttribute("todo", todo);
      response.setHeader("HX-Trigger", "clear-add-todo");
      return "todos/item";
    }
    return "redirect:/todos";
  }

  @PostMapping("/{id}")
  @Transactional
  public String edit(@PathVariable("id") UUID id, @RequestParam("title") String title) {
    Todo dbTodo = todos.findById(id).orElseThrow();
    dbTodo.title = title;
    // No explicit save: the entity is managed inside this transaction, so the UPDATE is
    // flushed on commit. This is the source's behaviour too — it also only assigned the
    // field inside a transaction.
    return "redirect:/todos";
  }

  @PostMapping("/toggle-all")
  @Transactional
  public String toggle() {
    boolean allCompleted = todos.countByCompletedFalse() == 0;
    todos.updateAllCompleted(!allCompleted);
    return "redirect:/todos";
  }

  @PostMapping("/{id}/toggle")
  @Transactional
  public String toggle(
      @PathVariable("id") UUID id,
      @RequestHeader(value = "HX-Request", defaultValue = "false") boolean hxRequest,
      Model model) {
    Todo todo = todos.findById(id).orElseThrow();
    todo.completed = !todo.completed;
    if (hxRequest) {
      model.addAttribute("todo", todo);
      return "todos/item";
    }
    return "redirect:/todos";
  }

  @PostMapping("/{id}/delete")
  @Transactional
  public String delete(@PathVariable("id") UUID id) {
    // The source's Panache deleteById returns false for a missing row rather than
    // throwing, and answers 302 either way. existsById keeps that: no 404, no 500.
    if (todos.existsById(id)) {
      todos.deleteById(id);
    }
    return "redirect:/todos";
  }

  @PostMapping("/clear-completed")
  @Transactional
  public String deleteCompleted() {
    todos.deleteCompleted();
    return "redirect:/todos";
  }
}
