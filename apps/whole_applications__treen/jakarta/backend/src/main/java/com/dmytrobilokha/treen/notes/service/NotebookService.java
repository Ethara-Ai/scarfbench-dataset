package com.dmytrobilokha.treen.notes.service;

import com.dmytrobilokha.treen.infra.db.DbException;
import com.dmytrobilokha.treen.infra.exception.InternalApplicationException;
import com.dmytrobilokha.treen.infra.exception.InvalidInputException;
import com.dmytrobilokha.treen.infra.exception.OptimisticLockException;
import com.dmytrobilokha.treen.notes.gpx.GpxPoint;
import com.dmytrobilokha.treen.notes.gpx.GpxRoot;
import com.dmytrobilokha.treen.notes.persistence.NewNote;
import com.dmytrobilokha.treen.notes.persistence.Note;
import com.dmytrobilokha.treen.notes.persistence.NoteRepository;
import com.dmytrobilokha.treen.notes.rest.NoteDto;
import com.dmytrobilokha.treen.notes.rest.NoteFlag;
import com.dmytrobilokha.treen.notes.rest.NotebookDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional(rollbackOn = Exception.class)
public class NotebookService {

    private NoteRepository noteRepository;

    public NotebookService() {
        //CDI
    }

    @Inject
    public NotebookService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public NotebookDto fetchUserNotebook(long userId) throws InternalApplicationException {
        List<Note> userNotes;
        try {
            userNotes = noteRepository.fetchNotes(userId);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to fetch user notes from the DB", e);
        }
        var notebook = new NotebookDto();
        notebook.setVersion(getNotebookVersion(userId, userNotes));
        var nodeById = new HashMap<Long, NoteDto>();
        var nodesByParentId = new HashMap<Long, List<NoteDto>>();
        var rootNodes = new ArrayList<NoteDto>();
        for (Note nodeEntity : userNotes) {
            var nodeDto = convertToDto(nodeEntity);
            var parentId = nodeEntity.getParentId();
            nodeById.put(nodeDto.getId(), nodeDto);
            if (parentId == null) {
                rootNodes.add(nodeDto);
            } else {
                nodesByParentId.computeIfAbsent(parentId, (parent) -> new ArrayList<>()).add(nodeDto);
            }
        }
        for (Map.Entry<Long, List<NoteDto>> entry : nodesByParentId.entrySet()) {
            var parentId = entry.getKey();
            var children = entry.getValue();
            Collections.sort(children);
            nodeById.get(parentId).setChildren(children);
        }
        Collections.sort(rootNodes);
        notebook.setNotes(rootNodes);
        return notebook;
    }

    private long getNotebookVersion(long userId, List<Note> userNotes) throws InternalApplicationException {
        if (userNotes.isEmpty()) {
            Long notebookVersion;
            try {
                notebookVersion = noteRepository.fetchNotebookVersion(userId);
            } catch (DbException e) {
                throw new InternalApplicationException("Failed to fetch notebook version", e);
            }
            if (notebookVersion == null) {
                throw new InternalApplicationException("Notebook version not found");
            }
            return notebookVersion;
        }
        return userNotes.get(0).getVersion();
    }

    private NoteDto convertToDto(Note note) {
        var noteDto = new NoteDto();
        noteDto.setId(note.getId());
        noteDto.setParentId(note.getParentId());
        noteDto.setTitle(note.getTitle());
        noteDto.setLink(note.getLink());
        noteDto.setFlags(convertFlags(note.getFlags()));
        noteDto.setDescription(note.getDescription());
        return noteDto;
    }

    private List<NoteFlag> convertFlags(long flags) {
        var result = new ArrayList<NoteFlag>();
        for (var flag : NoteFlag.values()) {
            if ((flags & (1L << flag.getBitPosition())) != 0) {
                result.add(flag);
            }
        }
        return result;
    }

    public NoteDto updateNote(Note note) throws InternalApplicationException, OptimisticLockException {
        int count;
        try {
            count = noteRepository.updateNote(note);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to update note in the DB", e);
        }
        if (count == 0) {
            throw new OptimisticLockException("Unable to update, your note is outdated");
        }
        return convertToDto(note);
    }

    public NoteDto createNoteNode(
            NewNote note) throws InternalApplicationException, OptimisticLockException {
        Long id;
        try {
            if (note.getParentId() == null) {
                id = noteRepository.insertRootNote(note);
            } else {
                id = noteRepository.insertNote(note);
            }
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to create the note in the DB", e);
        }
        if (id == null) {
            throw new OptimisticLockException("Unable to create a new note, your notebook is outdated");
        }
        incrementNotebookVersion(note.getUserId(), note.getVersion());
        return convertToDto(new Note(note, id));
    }

    public void removeNoteWithChildren(long noteId,
                                       long userId,
                                       long notebookVersion
    ) throws InternalApplicationException, OptimisticLockException {
        int count;
        try {
            count = noteRepository.deleteNote(noteId, userId, notebookVersion);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to delete a note from the DB", e);
        }
        if (count == 0) {
            throw new OptimisticLockException("Unable to delete a note, your notebook is outdated");
        }
        incrementNotebookVersion(userId, notebookVersion);
    }

    private void incrementNotebookVersion(
            long userId, long version) throws InternalApplicationException, OptimisticLockException {
        int count;
        try {
            count = noteRepository.incrementNotebookVersion(userId, version);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to increment the notebook version in the DB", e);
        }
        if (count == 0) {
            throw new OptimisticLockException("Unable to update the notebook, your version is outdated");
        }
    }

    public void moveNoteWithChildren(
            long noteId,
            @CheckForNull Long newParentId,
            long userId,
            long version) throws InternalApplicationException, InvalidInputException {
        if (newParentId == null) {
            moveNoteToRoot(noteId, userId, version);
        } else {
            moveNoteToNewParent(noteId, newParentId, userId, version);
        }
    }

    private void moveNoteToRoot(
            long noteId, long userId, long version) throws InternalApplicationException, OptimisticLockException {
        int count;
        try {
            count = noteRepository.moveNoteToRoot(noteId, userId, version);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to move the note", e);
        }
        if (count == 0) {
            throw new OptimisticLockException("Unable to move the note, your version is outdated");
        }
    }

    private void moveNoteToNewParent(
            long noteId,
            long newParentId,
            long userId,
            long version) throws InvalidInputException, InternalApplicationException {
        var notesGroup = fetchNoteFamily(noteId, userId);
        if (notesGroup == null) {
            throw new InvalidInputException("Notes don't exist");
        }
        var circleDetected = notesGroup.offspring()
                .stream()
                .anyMatch(n -> Objects.equals(n.getId(), newParentId));
        if (circleDetected) {
            throw new InvalidInputException("Notes should be a tree, not a circle");
        }
        int count;
        try {
            count = noteRepository.moveNote(noteId, newParentId, userId, version);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to move the note", e);
        }
        if (count == 0) {
            throw new OptimisticLockException("Unable to move the note, your version is outdated");
        }
    }

    @CheckForNull
    public GpxRoot exportChildren(long noteId, long userId) throws InternalApplicationException {
        var noteFamily = fetchNoteFamily(noteId, userId);
        if (noteFamily == null) {
            return null;
        }
        var points = noteFamily.offspring()
                .stream()
                .map(this::convertToGpxPoint)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GpxPoint::getName).thenComparing(GpxPoint::getDescription))
                .toList();
        return new GpxRoot(noteFamily.ancestor().getTitle(), points);
    }

    @CheckForNull
    private GpxPoint convertToGpxPoint(Note note) {
        var title = note.getTitle();
        if (title == null || title.isBlank()) {
            return null;
        }
        var link = note.getLink();
        if (link == null || !LinkPatternConstants.PROCESSED_GEO_LINK.matcher(link).matches()) {
            return null;
        }
        var linkCoordinates = link.substring(LinkPatternConstants.GEO_PREFIX_LENGTH).split(",");
        var lat = linkCoordinates[0];
        var lon = linkCoordinates[1];
        return new GpxPoint(lat, lon, title, note.getDescription());
    }

    @CheckForNull
    private NoteFamily fetchNoteFamily(long ancestorNoteId, long userId) throws InternalApplicationException {
        List<Note> userNotes;
        try {
            userNotes = noteRepository.fetchNotes(userId);
        } catch (DbException e) {
            throw new InternalApplicationException("Failed to fetch user notes from the DB", e);
        }
        var noteById = userNotes.stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));
        var ancestor = noteById.get(ancestorNoteId);
        if (ancestor == null) {
            return null;
        }
        var notesByParentId = userNotes.stream()
                .filter(n -> n.getParentId() != null)
                .collect(Collectors.groupingBy(Note::getParentId));
        var offspring = new ArrayList<Note>();
        var parentIds = new LinkedList<Long>();
        parentIds.add(ancestorNoteId);
        while (!parentIds.isEmpty()) {
            var parentId = parentIds.removeFirst();
            var childrenGroup = notesByParentId.getOrDefault(parentId, List.of());
            offspring.addAll(childrenGroup);
            childrenGroup.forEach(n -> parentIds.add(n.getId()));
        }
        return new NoteFamily(ancestor, offspring);
    }

    record NoteFamily(Note ancestor, List<Note> offspring) { }
}
