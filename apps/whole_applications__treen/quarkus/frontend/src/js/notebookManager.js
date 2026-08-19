define([
  'knockout',
  'appConstants',
  'appUtils'
],
  function (ko, AppConstants, AppUtils) {
    'use strict';

    class NotebookManager {

      buildNestedObservable(data) {
        //I want this method to work both for regular arrays and knockout observable arrays
        const dataArray = typeof data.map === 'function' ? data : data();
        return ko.observableArray(dataArray.map((node) => {
          const newNode = this.copyNoteIgnoringChildren(node);
          if (node.children) {
            newNode.children = this.buildNestedObservable(node.children);
          }
          return newNode;
        }));
      }

      copyNoteIgnoringChildren(note) {
        return {
          id: note.id,
          parentId: note.parentId,
          title: note.title,
          link: note.link,
          flags: note.flags,
          description: note.description,
        };
      }

      findNodePlace(topObservableArray, id) {
        const arrayData = topObservableArray();
        for (let i = 0; i < arrayData.length; i += 1) {
          if (arrayData[i].id == id) {
            const target = {
              enclosingArray: topObservableArray,
              index: i,
            };
            return target;
          }
          else if (arrayData[i].children) {
            const target = this.findNodePlace(arrayData[i].children, id);
            if (target) {
              return target;
            }
          }
        }
      }

      init() {
        return $.ajax({
          url: AppConstants.NOTEBOOK_URL,
        }).done((notebook) => {
          this.notebookVersion = notebook.version;
          this.notesTree(this.buildNestedObservable(notebook.notes)());
        });
      }

      getNodeSortingString(node) {
        if (AppUtils.isStringNotBlank(node.title)) {
          return node.title;
        }
        if (AppUtils.isStringNotBlank(node.link)) {
          return node.link;
        }
        if (AppUtils.isStringNotBlank(node.description)) {
          return node.description;
        }
        return '';
      }

      updateNote(note) {
        return $.ajax({
          url: `${AppConstants.NOTE_URL}/${note.id}`,
          type: 'PUT',
          contentType: 'application/json',
          data: JSON.stringify({
            parentId: note.parentId,
            title: note.title,
            link: note.link,
            flags: note.flags,
            description: note.description,
            version: this.notebookVersion
          })
        }).done((noteResponse) => {
          const targetLocation = this.findNodePlace(this.notesTree, note.id);
          const targetNode = targetLocation.enclosingArray()[targetLocation.index];
          const updatedNode = this.copyNoteIgnoringChildren(noteResponse);
          if (targetNode.children) {
            updatedNode.children = this.buildNestedObservable(targetNode.children);
          }
          targetLocation.enclosingArray.splice(targetLocation.index, 1);
          this.insertNoteOrdered(targetLocation.enclosingArray, updatedNode);
          this.notebookVersion++;
        });
      }

      createNote(note) {
        return $.ajax({
          url: AppConstants.NOTE_URL,
          type: 'POST',
          contentType: 'application/json',
          data: JSON.stringify({
            parentId: note.parentId,
            title: note.title,
            link: note.link,
            flags: note.flags,
            description: note.description,
            version: this.notebookVersion
          })
        }).done((noteResponse) => {
          const createdNote = this.copyNoteIgnoringChildren(noteResponse);
          this.attachNote(createdNote);
          this.notebookVersion++;
        });
      }

      attachNote(note) {
        let enclosingArray;
        if (note.parentId === undefined || note.parentId === null) {
          //Creating the root note
          enclosingArray = this.notesTree;
        } else {
          //Creating nested note
          const parentLocation = this.findNodePlace(this.notesTree, note.parentId);
          const parentNote = parentLocation.enclosingArray()[parentLocation.index];
          if (!parentNote.children) {
            const updatedParent = this.copyNoteIgnoringChildren(parentNote);
            updatedParent.children = ko.observableArray([note]);
            this.replaceNote(parentLocation, updatedParent);
            return;
          }
          enclosingArray = parentNote.children;
        }
        this.insertNoteOrdered(enclosingArray, note);
      }

      replaceNote(targetLocation, updatedNode) {
        if (targetLocation.enclosingArray().length === 1) {
          //If the array has only one element, we don't simply replace it, but add new - remove old to avoid parent node to shrink
          targetLocation.enclosingArray.push(updatedNode);
          targetLocation.enclosingArray.shift();
        } else {
          targetLocation.enclosingArray.splice(targetLocation.index, 1, updatedNode);
        }
      }

      insertNoteOrdered(observableOrderedArray, note) {
        const innerArray = observableOrderedArray();
        if (innerArray.length === 0 || this.compareNotes(innerArray[innerArray.length - 1], note) <= 0) {
          observableOrderedArray.push(note);
          return;
        }
        if (this.compareNotes(innerArray[0], note) > 0) {
          observableOrderedArray.unshift(note);
          return;
        }
        let startIndex = 0;
        let endIndex = innerArray.length - 1;
        while (endIndex - startIndex > 1) {
          const midIndex = (startIndex + endIndex) >>> 1;
          const cmp = this.compareNotes(innerArray[midIndex], note);
          if (cmp === 0) {
            //This should never happen, because we don't insert the same note into array already having it
            observableOrderedArray.splice(midIndex, 0, note);
            return;
          }
          if (cmp > 0) {
            endIndex = midIndex;
          } else {
            startIndex = midIndex;
          }
        }
        observableOrderedArray.splice(endIndex, 0, note);
      }

      deleteNote(note) {
        return $.ajax({
          url: `${AppConstants.NOTE_URL}/${note.id}`,
          type: 'DELETE',
          contentType: 'application/json',
          data: JSON.stringify({
            version: this.notebookVersion
          })
        }).done(() => {
          this.detachNote(note);
          this.notebookVersion++;
        });
      }

      exportGpx(note) {
        const downloadUrl = `${AppConstants.NOTE_URL}/${note.id}/export-children-gpx`;
        window.location = downloadUrl;
      }

      detachNote(note) {
        const targetLocation = this.findNodePlace(this.notesTree, note.id);
        if (targetLocation.enclosingArray().length === 1 && note.parentId) {
          //If we are removing the last child, we should set enclosing array to null for tree component to change the icon
          const parentLocation = this.findNodePlace(this.notesTree, note.parentId);
          const parentNote = parentLocation.enclosingArray()[parentLocation.index];
          const updatedParent = this.copyNoteIgnoringChildren(parentNote);
          this.replaceNote(parentLocation, updatedParent);
        } else {
          targetLocation.enclosingArray.splice(targetLocation.index, 1);
        }
      }

      moveNote(noteId, newParentId) {
        return $.ajax({
          url: `${AppConstants.NOTE_URL}/${noteId}`,
          type: 'PATCH',
          contentType: 'application/json',
          data: JSON.stringify({
            parentId: newParentId,
            version: this.notebookVersion
          })
        }).done(() => {
          const targetLocation = this.findNodePlace(this.notesTree, noteId);
          const targetNote = targetLocation.enclosingArray()[targetLocation.index];
          const movedNote = this.copyNoteIgnoringChildren(targetNote);
          movedNote.parentId = newParentId;
          if (targetNote.children) {
            movedNote.children = this.buildNestedObservable(targetNote.children);
          }
          this.detachNote(targetNote);
          this.attachNote(movedNote);
          this.notebookVersion++;
        });
      }

      sortNotesArray(notesArray) {
        notesArray.sort(this.compareNotes);
      }

      compareNotes(left, right) {
        const leftString = this.getNodeSortingString(left);
        const rightString = this.getNodeSortingString(right);
        if (leftString < rightString) {
          return -1;
        }
        if (leftString > rightString) {
          return 1;
        }
        if (left.id < right.id) {
          return -1;
        }
        if (left.id > right.id) {
          return 1;
        }
        return 0;
      }

      constructor() {
        this.notesTree = ko.observableArray();
        this.notebookVersion = 0;
        this.init = this.init.bind(this);
        this.updateNote = this.updateNote.bind(this);
        this.createNote = this.createNote.bind(this);
        this.deleteNote = this.deleteNote.bind(this);
        this.moveNote = this.moveNote.bind(this);
        this.exportGpx = this.exportGpx.bind(this);
      }

    }

    return new NotebookManager();
  });