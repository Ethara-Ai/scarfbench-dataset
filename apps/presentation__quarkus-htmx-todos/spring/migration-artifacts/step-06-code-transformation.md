# Step 06 — Code transformation (Gate E)

## Dependency injection (E1–E4)

The source has no service layer to scope: `TodoResource` is a JAX-RS resource with no
injection points at all, and every query goes through `static` methods on the entity.

| Source | Target |
|---|---|
| JAX-RS resource class, implicitly `@RequestScoped` under CDI | `@Controller` — a singleton, which is the target's convention for a stateless handler |
| `static` entity finders, no injection | constructor injection of `TodoRepository` |

E3 is **N/A**: no interface in this application has more than one implementation, so no
qualifier or producer was needed. E4 was checked at **runtime** — the application starts and
serves requests — rather than at compile time.

## REST routing (E5–E8)

| Source | Target |
|---|---|
| `@Path("/todos")` on the class | `@RequestMapping("/todos")` |
| `@GET` / `@POST` | `@GetMapping` / `@PostMapping` |
| `@PathParam("id") UUID` | `@PathVariable("id") UUID` |
| `@HeaderParam("HX-Request") boolean` | `@RequestHeader(value = "HX-Request", defaultValue = "false") boolean` |
| `@Form Todo todo` + `@FormParam` on the field | `@RequestParam("title") String title` |
| `TemplateInstance` return | view name + `Model` |
| `Response.status(FOUND).header("Location", "/todos")` | `"redirect:/todos"` |
| `Response.status(TEMPORARY_REDIRECT).header("Location", "/todos")` | `ResponseEntity.status(TEMPORARY_REDIRECT)` — **not** `"redirect:"`, which would send 302 |
| `Response.ok(template).header("HX-Trigger", …)` | view name + `HttpServletResponse.setHeader` |

**All eight routes keep their path and verb** (E8), verified by the oracle against both
variants.

Two routing details that did not need work, and one that did:

- **Literal versus templated paths.** `/todos/toggle-all` and `/todos/clear-completed` both
  also match `/todos/{id}`. JAX-RS prefers the literal path; the target's `PathPattern`
  comparator does the same. No reordering, no regex guard.
- **The header branch.** Both frameworks express it the same way — inject the header, branch
  in the method body — because JAX-RS cannot dispatch on a header value either. The branch
  had to reproduce *which* response carries `HX-Trigger`: the fragment does, the redirect
  does not.
- **URI normalisation.** See below. This is the one behaviour that had no target expression
  at all.

## Persistence and transactions (E9–E12)

The entity model is untouched (E9). The Panache statics become derived repository methods,
one for one:

```
Todo.listAll()                 findAll(Sort.ascending("createdTimestamp")).list()
  -> findAllByOrderByCreatedTimestampAsc()
Todo.listActive()              list("completed=false", Sort.ascending("createdTimestamp"))
  -> findByCompletedFalseOrderByCreatedTimestampAsc()
Todo.listCompleted()           list("completed=true",  Sort.ascending("createdTimestamp"))
  -> findByCompletedTrueOrderByCreatedTimestampAsc()
Todo.countActive()             count("completed != true")
  -> countByCompletedFalse()
Todo.updateAllCompleted(flag)  update("completed = ?1", completed)
  -> @Modifying @Query("update Todo t set t.completed = :completed")
Todo.deleteCompleted()         delete("completed = true")
  -> @Modifying @Query("delete from Todo t where t.completed = true")
```

Both bulk operations stay bulk: "toggle all" is a single `UPDATE` in the source and a single
`UPDATE` here, not a load-and-mutate loop.

`EntityManager` is not injected directly — the target's idiom is the repository, and the
source's idiom was the entity itself; neither side hand-manages a persistence context
(E11 satisfied by the target's own mechanism).

Transaction boundaries (E12): the source annotates each of the six write handlers with
`jakarta.transaction.Transactional`. The target annotates the same six with
`org.springframework.transaction.annotation.Transactional`. Same set of methods, no more and
no fewer.

Two write paths needed care:

- **`add`** uses `saveAndFlush`, not `save`. The htmx response renders `todo.id`, so the
  `INSERT` has to have happened before the view runs. The source got this from Panache's
  `persist` inside the same transaction.
- **`delete`** guards with `existsById`. The source's `Todo.deleteById` returns `false` for a
  missing row and answers 302; an unguarded `deleteById` would risk a 4xx/5xx for a request
  the source accepts. Measured: both variants answer **302** for an unknown id.

## Scheduling and startup (E20–E22)

E20 **N/A** — no scheduling. E21/E22: the only startup work is Flyway migration, and it
happens before the first request is served on both sides (`Successfully applied 2 migrations`
precedes the readiness line in both logs). There is no seed data to populate, which is what
makes "the list starts empty" a repeatable initial state rather than an accident.

## The one behaviour with no target expression: URI normalisation

Measured on the source: `GET /todos/` → 200, `GET /todos/active/` → 200, `GET //todos` → 200,
`POST /todos/toggle-all/` → 302. Measured on the target before the fix: **404 on all four**,
while every canonical route worked.

Nothing in the source *code* asks for this. It is the JAX-RS runtime normalising the request
URI before matching, and it disappears in a migration silently — the application looks
completely correct and four URL shapes that used to work stop working. Trailing-slash
matching was on by default through Spring Framework 5, deprecated in 6.0, and is gone.

Restored with `UriNormalizationFilter`: collapse repeated slashes, drop trailing ones, then
**forward** (not redirect — a redirect would turn a 200 into a 3xx and add a round trip the
source does not have). A filter rather than a second path on each mapping, because the
source's behaviour is uniform runtime normalisation, not eight hand-written aliases: writing
it once cannot drift out of step when a route is added.

The oracle asserts this (scenario 05), so it is now part of the graded contract rather than a
detail that happens to work.
