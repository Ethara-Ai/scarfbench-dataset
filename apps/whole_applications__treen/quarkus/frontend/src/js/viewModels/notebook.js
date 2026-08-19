define([
  'require',
  'exports',
  'knockout',
  'ojs/ojarraytreedataprovider',
  'notebookManager',
  'notificationManager',
  'appUtils',
  'loginManager',
  'ojs/ojknockout',
],
  function (require, exports, ko, ArrayTreeDataProvider, notebookManager, notificationManager, AppUtils, loginManager) {
    'use strict';

    class NotebookModel {

      constructor() {
        this.AppUtils = AppUtils;
        this.currentId = ko.observable();
        this.currentParentId = ko.observable();
        this.currentTitle = ko.observable('');
        this.currentLink = ko.observable('');
        this.currentFlags = ko.observableArray([]);
        this.currentDescription = ko.observable('');
        this.inputDisabled = ko.observable(false);
        this.titleErrors = ko.observableArray();
        this.linkErrors = ko.observableArray();
        this.notesLoading = ko.observable(true);
        this.clipBoardNoteData = ko.observable();

        this.openNoteAction = (e, d, con) => {
          if (!this.isPasteMode()) {
            this.openNoteDialog(d.data);
          }
          return true;
        };

        this.openNoteDialog = (note) => {
          this.currentId(note.id);
          this.currentParentId(note.parentId);
          this.currentTitle(note.title);
          this.currentLink(note.link);
          this.currentFlags(note.flags);
          this.currentDescription(note.description);
          document.getElementById('note-dialog').open();
        };

        this.isFormValid = () => {
          return AppUtils.isStringNotBlank(this.currentTitle())
            || AppUtils.isStringNotBlank(this.currentLink());
        }

        this.submitAction = () => {
          this.titleErrors.removeAll();
          this.linkErrors.removeAll();
          notificationManager.removeAllNotificationsOfType('note');
          if (!this.isFormValid()) {
            this.titleErrors.push({ summary: 'Invalid data', detail: 'Either title of link should not be empty', severity: 'error' });
            this.linkErrors.push({ summary: 'Invalid data', detail: 'Either title or link should not be empty', severity: 'error' });
            return true;
          }
          this.inputDisabled(true);
          const noteData = {
            id: this.currentId(),
            parentId: this.currentParentId(),
            title: AppUtils.isStringBlank(this.currentTitle()) ? null : this.currentTitle(),
            link: AppUtils.isStringBlank(this.currentLink()) ? null : this.currentLink(),
            flags: this.currentFlags(),
            description: AppUtils.isStringBlank(this.currentDescription()) ? null : this.currentDescription(),
          };
          const notebookAction = noteData.id ? notebookManager.updateNote(noteData) : notebookManager.createNote(noteData);
          notebookAction
            .fail(this.handleServerError)
            .done(() => {
              document.getElementById('note-dialog').close();
            }).always(() => {
              this.inputDisabled(false);
            });
          return true;
        };

        this.closeNoteDialog = () => {
          document.getElementById('note-dialog').close();
          return true;
        };

        this.createFirstNote = () => {
          this.openNewNoteDialog(null);
        };

        this.menuBeforeOpen = (event) => {
          const target = event.detail.originalEvent.target;
          this.currentNoteElementId = event.target.id;
          const treeView = document.getElementById("treeview");
          const context = treeView.getContextByNode(target);
          this.currentMenuNote = context ? {
            id: context.data.id,
            parentId: context.data.parentId,
            title: context.data.title,
            link: context.data.link,
            flags: context.data.flags,
            description: context.data.description,
            hasChildren: context.data.children ? true : false
          } : null;
        };

        this.openNewNoteDialog = (parentId) => {
          this.currentId(null);
          this.currentParentId(parentId);
          this.currentTitle(null);
          this.currentLink(null);
          this.currentFlags([]);
          this.currentDescription(null);
          document.getElementById('note-dialog').open();
        };

        this.menuAction = (event) => {
          const actionCode = event.target.value;
          const actionNote = this.currentMenuNote;
          if (actionNote) {
            switch (actionCode) {
              case 'edit':
                this.openNoteDialog(actionNote);
                break;

              case 'addChild':
                this.openNewNoteDialog(actionNote.id);
                break;

              case 'addSibling':
                this.openNewNoteDialog(actionNote.parentId);
                break;

              case 'delete':
                if (actionNote.hasChildren) {
                  this.openDeleteDialog(actionNote);
                } else {
                  notebookManager.deleteNote(actionNote)
                    .fail(this.handleServerError);
                }
                break;

              case 'cut':
                this.cutNote(actionNote);
                break;

              case 'exportGpx':
                notebookManager.exportGpx(actionNote);
                break;

              case 'cancelCut':
                this.cleanCutNote();
                break;

              case 'pasteChild':
                this.pasteNote(this.clipBoardNoteData(), actionNote.id);
                break;

              case 'pasteSibling':
                this.pasteNote(this.clipBoardNoteData(), actionNote.parentId);
                break;
            }
          }
        };

        this.openDeleteDialog = (note) => {
          this.currentId(note.id);
          this.currentParentId(note.parentId);
          this.currentTitle(note.title);
          this.currentLink(note.link);
          this.currentFlags(note.flags);
          this.currentDescription(note.description);
          document.getElementById('delete-dialog').open();
          return true;
        };

        this.deleteNoteFromDialog = () => {
          notebookManager.deleteNote({
            id: this.currentId(),
            parentId: this.currentParentId(),
            title: this.currentTitle(),
            link: this.currentLink(),
            flags: this.currentFlags(),
            description: this.currentDescription(),
          })
          .fail(this.handleServerError)
          .done(this.closeDeleteDialog);
        };

        this.closeDeleteDialog = () => {
          document.getElementById('delete-dialog').close();
          return true;
        };

        this.handleServerError = (jqXHR, textStatus, errorThrown) => {
          let serverMessage = null;
          if (jqXHR.responseJSON) {
            serverMessage = jqXHR.responseJSON.message;
          }
          if (errorThrown === 'Unauthorized') {
            loginManager.registerFailedAuthorization();
            notificationManager.addNotification({
              severity: 'error',
              summary: 'Authorization error',
              detail: serverMessage ? serverMessage : 'You need to login to access the notebook',
              type: 'login'
            });
          } else {
            notificationManager.addNotification({
              severity: 'error',
              summary: 'Operation has failed',
              detail: serverMessage ? serverMessage : `${textStatus} - ${errorThrown}`,
              type: 'note'
            });
          }
        };

        this.cutNote = (note) => {
          this.clipBoardNoteData({ id: note.id, parentId: note.parentId});
        };

        this.cleanCutNote = () => {
          this.clipBoardNoteData(null);
        };

        this.pasteNote = (noteData, parentId) => {
          if (noteData.parentId === parentId) {
            this.cleanCutNote();
            return;
          }
          notebookManager.moveNote(noteData.id, parentId)
            .fail(this.handleServerError)
            .always(this.cleanCutNote);
        };

        this.connected = () => {
          this.notesLoading(true);
          notebookManager.init()
            .fail(this.handleServerError)
            .always(this.notesLoading(false));
        };

        this.getNoteIconStyleClass = (noteId) => {
          const noteData = this.clipBoardNoteData();
          if (noteData != null && noteId === noteData.id) {
            return "oj-text-color-disabled";
          }
        };

        this.isPasteMode = () => {
          return this.clipBoardNoteData() != null;
        }

        this.notesProvider = new ArrayTreeDataProvider(notebookManager.notesTree);
      }

    }

    return new NotebookModel();
  });