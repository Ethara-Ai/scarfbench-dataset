# Step 05 — Preserve / rewrite decisions (Gate D)

| Source file | Decision | Why |
|---|---|---|
| `Todo.java` | **preserved, minus three source-framework couplings** | D1/D2/E9. Field names, column names, types, the UUID primary key, `@CreationTimestamp` and both accessors are the source's. Removed: `extends PanacheEntityBase`, the six `static` finders, and `@org.jboss.resteasy.annotations.jaxrs.FormParam` on `title`. Each of those is a source-framework runtime assumption living on the entity (D10) |
| — | **added** `TodoRepository.java` | E10. The six removed statics, re-expressed as an explicit repository. One method per source method, documented against the Panache call it replaces |
| `AppResource.java` | **rewritten** as `AppController.java` | D5. One route, one status code, and the status matters: 307, not the 302 the target's redirect idiom would produce |
| `TodoResource.java` | **rewritten** as `TodoController.java` | D5. Eight routes, all paths and verbs unchanged (E8) |
| `templates/base.html` | **rewritten** | D8. Qute's insert/include decorator → a Thymeleaf parameterised-fragment layout |
| `templates/TodoResource/list.html` | **rewritten** as `templates/todos/list.html` | D8 |
| `templates/TodoResource/item.html` | **rewritten** as `templates/todos/item.html` | D8 |
| `META-INF/resources/todos.js` | **preserved, byte-identical**, moved to `static/` | D4. Framework-neutral front-end source; only the classpath location the runtime serves from changes |
| `db/migration/V1…sql`, `V2…sql` | **preserved, byte-identical** | D4. Framework-neutral SQL, run by the same tool at the same point in startup |
| — | **added** `TodosApplication.java` | D7. The target needs an entry point; the source needs none, so nothing was preserved here |
| — | **added** `UriNormalizationFilter.java` | Restores behaviour the *source runtime* provided and the target does not. See step 06 |
| `application.properties` | **replaced** | D7, step 04 |
| `pom.xml` | **replaced** | step 03 |
| — | no tests migrated | G7 |

## Template directory naming

`templates/TodoResource/` → `templates/todos/`. The source path is not a free choice on its
side: `@CheckedTemplate` derives it from the enclosing class name, so `TodoResource/` is a
*source-framework convention* encoded as a directory. Carrying the name over would preserve
the convention of a framework that is no longer present (D10), and the path is not
observable at the HTTP boundary — no response references it. The package namespace `todos`,
which *is* benchmark identity, is retained unchanged (D9).

## Nothing framework-neutral was rewritten (D11)

`todos.js`, both migration scripts and both webjar versions are byte-identical to the
source's. The entity's field and column names are unchanged, so the persisted schema and the
data in it are the same on both sides — the same `V1`/`V2` scripts produce the same table.
