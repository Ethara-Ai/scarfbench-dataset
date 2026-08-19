package com.dmytrobilokha.treen.notes.rest;

import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.infra.exception.InvalidInputException;
import com.dmytrobilokha.treen.login.rest.UserSessionData;
import com.dmytrobilokha.treen.notes.service.NotebookService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Objects;

@RequestScoped
@Path("notebook")
public class NotebookResource {

    private NotebookService notebookService;
    private NoteRequestConvertor requestConvertor;
    private UserSessionData userSessionData;

    public NotebookResource() {
        //CDI
    }

    @Inject
    public NotebookResource(
            NotebookService notebookService,
            NoteRequestConvertor requestConvertor,
            UserSessionData userSessionData) {
        this.notebookService = notebookService;
        this.requestConvertor = requestConvertor;
        this.userSessionData = userSessionData;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public NotebookDto getNotebook() throws InternalApplicationException {
        return notebookService.fetchUserNotebook(userSessionData.getAuthenticatedUserId());
    }

    @PUT
    @Path("note/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public NoteDto updateNote(
            @PathParam("id") @NotNull(message = "Note id must be provided") Long id,
            @Valid ChangeNoteRequest request) throws InternalApplicationException, InvalidInputException {
        var note = requestConvertor.convertUpdateRequest(id, request);
        return notebookService.updateNote(note);
    }

    @POST
    @Path("note")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public NoteDto createNote(
            @Valid ChangeNoteRequest request) throws InternalApplicationException, InvalidInputException {
        var note = requestConvertor.convertCreateRequest(request);
        return notebookService.createNoteNode(note);
    }

    @DELETE
    @Path("note/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteNote(
            @PathParam("id") @NotNull(message = "Note id must be provided") Long id,
            @Valid DeleteNoteRequest request
    ) throws InternalApplicationException, InvalidInputException {
        notebookService.removeNoteWithChildren(
                id, userSessionData.getAuthenticatedUserId(), request.getVersion());
        return Response.ok().build();
    }

    @PATCH
    @Path("note/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response moveNote(
            @PathParam("id") @NotNull(message = "Note id must be provided") Long id,
            @Valid MoveNoteRequest request) throws InternalApplicationException, InvalidInputException {
        notebookService.moveNoteWithChildren(
                id,
                request.getParentId(),
                userSessionData.getAuthenticatedUserId(),
                request.getVersion()
        );
        return Response.ok().build();
    }

    @GET
    @Path("note/{id}/export-children-gpx")
    @Produces("application/gpx+xml")
    public Response exportChildrenToGpx(
            @PathParam("id") @NotNull(message = "Note id must be provided") Long id
    ) throws InternalApplicationException, InvalidInputException {
        var gpxExport = notebookService.exportChildren(id, userSessionData.getAuthenticatedUserId());
        if (gpxExport == null) {
            throw new InvalidInputException("Note with provided id doesn't exist");
        }
        return Response.ok(gpxExport)
                .header("Content-Disposition", "attachment; filename=\""
                            + Objects.requireNonNullElse(gpxExport.getCollectionName(), "note") + "_points.gpx\"")
                .build();
    }

}
