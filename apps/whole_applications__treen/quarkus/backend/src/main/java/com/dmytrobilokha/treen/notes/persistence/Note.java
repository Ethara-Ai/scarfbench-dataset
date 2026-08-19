package com.dmytrobilokha.treen.notes.persistence;

import javax.annotation.CheckForNull;

public class Note extends NewNote {

    private final long id;

    public Note(
            long id,
            @CheckForNull Long parentId,
            long userId,
            @CheckForNull String title,
            @CheckForNull String link,
            long flags,
            @CheckForNull String description,
            long version) {
        super(parentId, userId, title, link, flags, description, version);
        this.id = id;
    }

    public Note(NewNote newNote, long id) {
        super(newNote);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Note{"
                + "id=" + id
                + ", parentId=" + parentId
                + ", userId=" + userId
                + ", title='" + title + '\''
                + ", link='" + link + '\''
                + ", flags=" + Long.toBinaryString(flags)
                + ", description='" + description + '\''
                + ", version=" + version
                + '}';
    }

}
