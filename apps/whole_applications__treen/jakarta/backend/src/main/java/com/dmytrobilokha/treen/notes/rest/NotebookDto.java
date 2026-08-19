package com.dmytrobilokha.treen.notes.rest;

import java.util.List;

public class NotebookDto {

    private long version;
    private List<NoteDto> notes = List.of();

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<NoteDto> getNotes() {
        return notes;
    }

    public void setNotes(List<NoteDto> notes) {
        this.notes = notes;
    }

}
