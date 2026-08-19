package com.dmytrobilokha.treen.infra;

import com.dmytrobilokha.treen.infra.rest.ExceptionResponse;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = "/error")
public class ErrorHandlingServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlingServlet.class);
    private static final Jsonb JSONB = JsonbBuilder.create();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        try {
            var statusCode = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            if (statusCode == null) {
                statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            resp.setStatus(statusCode);
            var originalMessage = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
            var exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            var requestUri = (String) req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            var servletName = (String) req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
            String userMessage;
            if (statusCode >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                userMessage = "Unexpected internal error happened";
                LOG.error("Unexpected internal error/exception happened for"
                                + " requested uri '{}' in servlet '{}' with '{}' message",
                        requestUri, servletName, originalMessage, exception);
            } else {
                userMessage = originalMessage;
                LOG.warn("User action caused an error/exception for"
                                + " requested uri '{}' in servlet '{}' with '{}' message and code {}",
                        requestUri, servletName, originalMessage, statusCode, exception);
            }
            if (isJsonAccepted(req)) {
                var errorResponse = new ExceptionResponse(userMessage);
                resp.setContentType(MediaType.APPLICATION_JSON);
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                var responseWriter = resp.getWriter();
                JSONB.toJson(errorResponse, responseWriter);
                return;
            }
            resp.setContentType(MediaType.TEXT_HTML);
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            printErrorHtmlPage(statusCode, userMessage, resp.getWriter());
        } catch (RuntimeException | IOException e) {
            LOG.error("Got an exception while handling another webapp exception", e);
        }
    }

    private boolean isJsonAccepted(HttpServletRequest request) {
        for (var iterator = request.getHeaders("Accept").asIterator(); iterator.hasNext();) {
            var acceptHeader = iterator.next();
            if (acceptHeader.startsWith(MediaType.APPLICATION_JSON)) {
                return true;
            }
        }
        return false;
    }

    private void printErrorHtmlPage(int statusCode, String message, PrintWriter writer) {
        writer.print("<html><head><title>Error</title></head><body>");
        writer.print("<h2 style=\"color:grey;background-color:tomato;\">Error ");
        writer.print(statusCode);
        writer.print(" - ");
        writer.print(message);
        writer.print("</h2></body></html>");
    }

}
