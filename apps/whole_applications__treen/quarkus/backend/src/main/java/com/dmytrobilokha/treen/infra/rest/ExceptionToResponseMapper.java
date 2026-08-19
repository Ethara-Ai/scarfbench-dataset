package com.dmytrobilokha.treen.infra.rest;

import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.infra.exception.InvalidInputException;
import com.dmytrobilokha.treen.infra.exception.OptimisticLockException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class ExceptionToResponseMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionToResponseMapper.class);
    private static final Map<Class<? extends Exception>, Response.Status> APP_EXCEPTION_STATUS = Map.of(
            InternalApplicationException.class, Response.Status.INTERNAL_SERVER_ERROR,
            OptimisticLockException.class, Response.Status.PRECONDITION_FAILED,
            InvalidInputException.class, Response.Status.BAD_REQUEST);

    @Override
    public Response toResponse(Exception exception) {
        var status = APP_EXCEPTION_STATUS.get(exception.getClass());
        if (status != null) {
            return logAndConvert(exception, status, exception.getMessage());
        }
        for (Map.Entry<Class<? extends Exception>, Response.Status> entry : APP_EXCEPTION_STATUS.entrySet()) {
            if (entry.getKey().isAssignableFrom(exception.getClass())) {
                return logAndConvert(exception, entry.getValue(), exception.getMessage());
            }
        }
        return logAndConvert(exception, Response.Status.INTERNAL_SERVER_ERROR, "Unknown internal exception");
    }

    private Response logAndConvert(Exception e, Response.Status status, String message) {
        LOG.error("Exception thrown in JAX-RS resource", e);
        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ExceptionResponse(message))
                .build();
    }

}
