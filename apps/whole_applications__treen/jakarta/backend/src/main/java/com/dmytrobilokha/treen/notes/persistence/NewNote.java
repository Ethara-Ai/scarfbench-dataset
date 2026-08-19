package com.dmytrobilokha.treen.notes.persistence;

import javax.annotation.CheckForNull;

public class NewNote {

    @CheckForNull
    protected final Long parentId;
    protected final long userId;
    @CheckForNull
    protected final String title;
    @CheckForNull
    protected final String link;
    protected final long flags;
    @CheckForNull
    protected final String description;
    protected final long version;

    public NewNote(
            @CheckForNull Long parentId,
            long userId,
            @CheckForNull String title,
            @CheckForNull String link,
            long flags,
            @CheckForNull String description,
            long version) {
        this.parentId = parentId;
        this.userId = userId;
        this.title = title;
        this.link = link;
        this.flags = flags;
        this.description = description;
        this.version = version;
    }

    public NewNote(NewNote original) {
        this.parentId = original.parentId;
        this.userId = original.userId;
        this.title = original.title;
        this.link = original.link;
        this.flags = original.flags;
        this.description = original.description;
        this.version = original.version;
    }

    @CheckForNull
    public Long getParentId() {
        return parentId;
    }

    public long getUserId() {
        return userId;
    }

    @CheckForNull
    public String getTitle() {
        return title;
    }

    @CheckForNull
    public String getLink() {
        return link;
    }

    @CheckForNull
    public String getDescription() {
        return description;
    }

    public long getFlags() {
        return flags;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "NewNote{"
                + ", parentId=" + parentId
                + ", userId=" + userId
                + ", title='" + title + '\''
                + ", link='" + link + '\''
                + ", flags=" + Long.toBinaryString(flags)
                + ", description='" + description + '\''
                + '}';
    }

}
