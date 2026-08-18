package todos;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restores a behaviour the source got from its runtime and the target does not provide.
 *
 * <p>The source's HTTP layer normalises the request URI before matching, so every route is
 * reachable with a trailing slash and with repeated slashes — measured against the running
 * source variant:
 *
 * <pre>
 *   GET  /todos/                 200      GET  /todos/active/            200
 *   GET  //todos                 200      POST /todos/toggle-all/        302
 * </pre>
 *
 * <p>Spring MVC matches the URI as given. Trailing-slash matching was on by default through
 * Spring Framework 5, deprecated in 6.0 and is gone: without this filter all four requests
 * above answer 404 while every route in the application still works, which is exactly the
 * kind of regression that survives a migration unnoticed — nothing in the source *code* says
 * "also accept a trailing slash", so there is nothing to port. See FINDINGS.md §4.1.
 *
 * <p>It is a filter rather than a second path on each mapping deliberately. The source's
 * behaviour is URI normalisation applied uniformly by the runtime, not eight hand-written
 * aliases; expressing it once keeps the mapping declarations identical to the source's and
 * cannot drift out of step when a route is added.
 *
 * <p>Normalised requests are forwarded rather than redirected: a redirect would change the
 * observable status code from 200 to 3xx and force a second round trip, which the source
 * does not do.
 */
@Component
public class UriNormalizationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    String normalized = normalize(uri);
    if (!normalized.equals(uri)) {
      String query = request.getQueryString();
      request
          .getRequestDispatcher(query == null ? normalized : normalized + "?" + query)
          .forward(request, response);
      return;
    }
    chain.doFilter(request, response);
  }

  /** Collapses repeated slashes and drops trailing ones. The root path stays "/". */
  static String normalize(String uri) {
    String result = uri.replaceAll("/{2,}", "/");
    while (result.length() > 1 && result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result.isEmpty() ? "/" : result;
  }
}
