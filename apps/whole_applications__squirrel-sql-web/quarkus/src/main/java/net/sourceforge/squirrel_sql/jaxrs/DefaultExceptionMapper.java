package net.sourceforge.squirrel_sql.jaxrs;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.squirrel_sql.dto.ExceptionBean;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Exception> {

    Logger logger = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @Override
    public Response toResponse(Exception e) {

        e = extractParentException(e);
        ExceptionContent excContent = extractMessage(e);

        if (excContent.shouldBeLogged()) {
            logger.error("Internal server error:", e);
        }

        ExceptionBean excBean = new ExceptionBean(excContent.getHttpStatus(), excContent.getMessage());
        return Response.status(excContent.getHttpStatus()) //
                .entity(excBean)//
                .type(MediaType.APPLICATION_JSON)//
                .build();
    }

    /**
     * Skip usually unuseful exceptions
     * 
     * @param e
     * @return
     */
    public Exception extractParentException(Exception e) {

        // Migration note (jakarta -> quarkus): the EJB/JPA/JTA container exception
        // wrappers no longer apply once EJB is replaced by CDI, so we only unwrap the
        // Hibernate JDBC wrapper that the SQuirreL core may still raise.
        while ((e instanceof org.hibernate.exception.GenericJDBCException) && e.getCause() != null
                && e.getCause() instanceof Exception) {
            e = (Exception) e.getCause();
        }

        return e;
    }

    /**
     * Extract human-readable data from exception
     * 
     * @param e
     * @return
     */
    public ExceptionContent extractMessage(Exception e) {

        ExceptionContent ret = new ExceptionContent();

        if (e instanceof WebApplicationException) {
            // don't print stack trace, if client issue
            // Unluckily, some library prints it anyway.
            ret.httpStatus = Status.fromStatusCode(((WebApplicationException) e).getResponse().getStatus());
        } else if (e instanceof IllegalArgumentException) {
            ret.httpStatus = Status.BAD_REQUEST;
        } else {
            ret.httpStatus = Status.INTERNAL_SERVER_ERROR;
        }

        ret.message = (e.getMessage() != null) ? e.getMessage() : ret.httpStatus.getReasonPhrase();
        ret.shouldBeLogged = (ret.httpStatus == Status.INTERNAL_SERVER_ERROR);

        return ret;
    }

    public static class ExceptionContent {
        private Status httpStatus;
        private String message;
        private boolean shouldBeLogged = false;

        public Status getHttpStatus() {
            return httpStatus;
        }

        public String getMessage() {
            return message;
        }

        public boolean shouldBeLogged() {
            return shouldBeLogged;
        }
    }
}
