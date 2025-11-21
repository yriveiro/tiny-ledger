package com.acme.api.ledger.mapper;

import com.acme.api.ledger.apispec.dto.InvalidResponse;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NullPointExceptionMapper implements ExceptionMapper<NullPointerException> {
    private static final String JSON_BODY_REQUIRED = "JSON body is required";
    private static final String INTERNAL_SERVER_ERROR = "Internal server error";

    @Override
    public Response toResponse(final NullPointerException exception) {
        Log.infof("NPE: %s", exception.getLocalizedMessage());

        return switch (exception.getMessage()) {
            case JSON_BODY_REQUIRED ->
                buildResponse(Response.Status.BAD_REQUEST, "Invalid request: " + exception.getMessage());
            default ->
                buildResponse(Response.Status.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
        };
    }

    private Response buildResponse(Response.Status status, String message) {
        final InvalidResponse err = new InvalidResponse();

        err.setMessage(message);

        return Response.status(status).entity(err).build();
    }
}
