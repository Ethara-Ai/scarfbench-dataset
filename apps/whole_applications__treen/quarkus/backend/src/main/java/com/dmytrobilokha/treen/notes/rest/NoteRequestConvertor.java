package com.dmytrobilokha.treen.notes.rest;

import com.dmytrobilokha.treen.login.rest.UserSessionData;
import com.dmytrobilokha.treen.notes.persistence.NewNote;
import com.dmytrobilokha.treen.notes.persistence.Note;
import com.dmytrobilokha.treen.notes.service.LinkType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.annotation.CheckForNull;
import java.util.List;

@ApplicationScoped
public class NoteRequestConvertor {

    private UserSessionData userSessionData;

    public NoteRequestConvertor() {
        //CDI
    }

    @Inject
    public NoteRequestConvertor(UserSessionData userSessionData) {
        this.userSessionData = userSessionData;
    }

    NewNote convertCreateRequest(ChangeNoteRequest request) {
        return new NewNote(
                request.getParentId(),
                userSessionData.getAuthenticatedUserId(),
                request.getTitle(),
                convertLink(request.getLink()),
                convertFlags(request.getFlags()),
                request.getDescription(),
                request.getVersion()
        );
    }

    Note convertUpdateRequest(long id, ChangeNoteRequest request) {
        return new Note(
                id,
                request.getParentId(),
                userSessionData.getAuthenticatedUserId(),
                request.getTitle(),
                convertLink(request.getLink()),
                convertFlags(request.getFlags()),
                request.getDescription(),
                request.getVersion()
        );
    }

    @CheckForNull
    private String convertLink(@CheckForNull String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        return LinkType.detect(link).format(link);
    }

    private long convertFlags(@CheckForNull List<NoteFlag> flags) {
        long result = 0L;
        if (flags == null) {
            return result;
        }
        for (var flag : flags) {
            result |= 1L << flag.getBitPosition();
        }
        return result;
    }

}
