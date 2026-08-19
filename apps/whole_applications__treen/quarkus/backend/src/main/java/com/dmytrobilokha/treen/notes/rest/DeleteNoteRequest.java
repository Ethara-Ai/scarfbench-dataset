package com.dmytrobilokha.treen.notes.rest;

import jakarta.validation.constraints.NotNull;

public class DeleteNoteRequest {

    @NotNull(message = "Version must be provided")
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
