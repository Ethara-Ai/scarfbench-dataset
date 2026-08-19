package com.dmytrobilokha.treen.notes.persistence;

import com.dmytrobilokha.treen.infra.db.DbException;
import com.dmytrobilokha.treen.infra.db.DbQueryExecutor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.annotation.CheckForNull;
import java.util.List;

@ApplicationScoped
public class NoteRepository {

    private DbQueryExecutor queryExecutor;

    public NoteRepository() {
        //CDI
    }

    @Inject
    public NoteRepository(DbQueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public List<Note> fetchNotes(long notesOwnerId) throws DbException {
        return queryExecutor.selectList(new FetchNotesQuery(notesOwnerId));
    }

    @CheckForNull
    public Long fetchNotebookVersion(long notesOwnerId) throws DbException {
        return queryExecutor.selectObject(new FetchNotebookVersionQuery(notesOwnerId));
    }

    public int updateNote(Note noteNode) throws DbException {
        return queryExecutor.update(new UpdateNoteQuery(noteNode));
    }

    public int incrementNotebookVersion(long userId, long notebookVersion) throws DbException {
        return queryExecutor.update(new IncrementNotebookVersionQuery(userId, notebookVersion));
    }

    @CheckForNull
    public Long insertNote(NewNote note) throws DbException {
        return queryExecutor.insert(new InsertNoteQuery(note));
    }

    @CheckForNull
    public Long insertRootNote(NewNote note) throws DbException {
        return queryExecutor.insert(new InsertRootNoteQuery(note));
    }

    public int deleteNote(long id, long userId, long version) throws DbException {
        return queryExecutor.delete(new DeleteNoteQuery(id, userId, version));
    }

    public int moveNote(long id, long newParentId, long userId, long version) throws DbException {
        return queryExecutor.update(new MoveNoteQuery(id, newParentId, userId, version));
    }

    public int moveNoteToRoot(long id, long userId, long version) throws DbException {
        return queryExecutor.update(new MoveNoteToRootQuery(id, userId, version));
    }

}
