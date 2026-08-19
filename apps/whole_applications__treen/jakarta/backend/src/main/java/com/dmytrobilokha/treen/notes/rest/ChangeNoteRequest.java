package com.dmytrobilokha.treen.notes.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import javax.annotation.CheckForNull;
import java.util.List;

@TitleOrLinkRequired
public class ChangeNoteRequest {

    @CheckForNull
    private Long parentId;
    @Size(max = 150, message = "Title should be no longer than 150 characters")
    @CheckForNull
    private String title;
    @Size(max = 8192, message = "Link should be no longer than 8192 characters")
    @CheckForNull
    private String link;
    @CheckForNull
    private List<NoteFlag> flags;
    @Size(max = 2000, message = "Description should be no longer than 2000 characters")
    @CheckForNull
    private String description;
    @NotNull(message = "Version must be provided")
    private Long version;

    @CheckForNull
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(@CheckForNull Long parentId) {
        this.parentId = parentId;
    }

    @CheckForNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@CheckForNull String title) {
        this.title = title;
    }

    @CheckForNull
    public String getLink() {
        return link;
    }

    public void setLink(@CheckForNull String link) {
        this.link = link;
    }

    @CheckForNull
    public List<NoteFlag> getFlags() {
        return flags;
    }

    public void setFlags(@CheckForNull List<NoteFlag> flags) {
        this.flags = flags;
    }

    @CheckForNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@CheckForNull String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
