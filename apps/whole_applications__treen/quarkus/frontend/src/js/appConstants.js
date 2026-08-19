define([],
  function() {
    'use strict';

    class AppConstants {

      addConstant(name, value) {
        Object.defineProperty(this, name, {
          value: value,
          enumerable: true
        });
      }

      constructor() {
        this.addConstant('CONTEXT_PATH', '/treen');
        this.addConstant('NOTEBOOK_URL', `${this.CONTEXT_PATH}/service/notebook`);
        this.addConstant('NOTE_URL', `${this.NOTEBOOK_URL}/note`);
        this.addConstant('USER_URL', `${this.CONTEXT_PATH}/service/user`);
        this.addConstant('AUTH_URL', `${this.CONTEXT_PATH}/service/auth`);
      }

    }

    return new AppConstants();
  });