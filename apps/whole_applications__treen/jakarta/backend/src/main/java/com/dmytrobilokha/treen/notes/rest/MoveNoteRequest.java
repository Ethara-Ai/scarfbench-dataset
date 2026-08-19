package com.dmytrobilokha.treen.notes.rest;

import jakarta.validation.constraints.NotNull;

import javax.annotation.CheckForNull;

public class MoveNoteRequest {

    @CheckForNull
    private Long parentId;
    @NotNull(message = "Version must be provided")
    private Long version;

    @CheckForNull
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(@CheckForNull Long parentId) {
        this.parentId = parentId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
