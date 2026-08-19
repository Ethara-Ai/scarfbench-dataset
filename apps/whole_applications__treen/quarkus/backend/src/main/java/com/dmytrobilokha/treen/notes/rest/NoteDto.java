package com.dmytrobilokha.treen.notes.rest;

import javax.annotation.CheckForNull;
import java.util.List;

public class NoteDto implements Comparable<NoteDto> {

    private long id;
    @CheckForNull
    private Long parentId;
    @CheckForNull
    private String title;
    @CheckForNull
    private String link;
    @CheckForNull
    private String description;
    private List<NoteFlag> flags;
    @CheckForNull
    private List<NoteDto> children;

    @Override
    public int compareTo(NoteDto o) {
        int baseResult = getComparisonBase().compareTo(o.getComparisonBase());
        if (baseResult != 0) {
            return baseResult;
        }
        return Long.compare(id, o.id);
    }

    private String getComparisonBase() {
        if (title != null) {
            return title;
        }
        if (link != null) {
            return link;
        }
        if (description != null) {
            return description;
        }
        //This should never happen
        return "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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
    public String getDescription() {
        return description;
    }

    public void setDescription(@CheckForNull String description) {
        this.description = description;
    }

    public List<NoteFlag> getFlags() {
        return flags;
    }

    public void setFlags(List<NoteFlag> flags) {
        this.flags = flags;
    }

    @CheckForNull
    public List<NoteDto> getChildren() {
        return children;
    }

    public void setChildren(List<NoteDto> children) {
        this.children = children;
    }

}
