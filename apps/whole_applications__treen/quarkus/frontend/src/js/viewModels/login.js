define([
  'require',
  'exports',
  'knockout',
  'loginManager',
  'notificationManager',
  'ojs/ojbootstrap',
  'ojs/ojknockout',
  'ojs/ojinputtext',
  'ojs/ojlabel',
  'ojs/ojformlayout',
  'ojs/ojbutton',
  'ojs/ojcheckboxset'
],
  function (require, exports, ko, loginManager, notificationManager) {
    'use strict';

    class LoginModel {

      constructor() {
        this.loginText = ko.observable('');
        this.loginErrors = ko.observableArray();
        this.passwordText = ko.observable('');
        this.passwordErrors = ko.observableArray();
        this.rememberMeSet = ko.observableArray(['rememberMe']);
        this.inputDisabled = ko.observable(false);
        
        this.passwordTypeAction = (e) => {
          if (e.detail.originalEvent.key === "Enter") {
            this.loginAction();
          }
          return true;
        };

        this.loginAction = () => {
          this.loginErrors.removeAll();
          this.passwordErrors.removeAll();
          let nonValidForm = false;
          if (this.loginText() === '') {
            nonValidForm = true;
            this.loginErrors.push({ summary: 'Invalid login', detail: 'Login should not be empty', severity: 'error' });
          }
          if (this.passwordText() === '') {
            nonValidForm = true;
            this.passwordErrors.push({ summary: 'Invalid password', detail: 'Password should not be empty', severity: 'error' });
          }
          if (nonValidForm) {
            return true;
          }
          this.inputDisabled(true);
          const rememberMe = this.rememberMeSet().length > 0;
          notificationManager.removeAllNotificationsOfType('login');
          loginManager
            .login(this.loginText(), this.passwordText(), rememberMe)
            .done(() => {
              this.loginText('');
              this.passwordText('');
            })
            .fail((jqXHR, textStatus, errorThrown) => {
              let serverMessage = null;
              if (jqXHR.responseJSON) {
                serverMessage = jqXHR.responseJSON.message;
              }
              notificationManager.addNotification({
                severity: 'error',
                summary: 'Login failed',
                detail: serverMessage ? serverMessage : `${textStatus} - ${errorThrown}`,
                type: 'login'
              });
            })
            .always(() => {
              this.inputDisabled(false);
            });
          return true;
        };
      }

    }

    return new LoginModel();
  });