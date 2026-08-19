define([],
  function() {
    'use strict';

    class AppUtils {

      isStringNotBlank(text) {
        return text !== undefined && text !== null && text.trim().length > 0;
      }

      isStringBlank(text) {
        return !this.isStringNotBlank(text);
      }

      constructor() {
        this.isStringNotBlank = this.isStringNotBlank.bind(this);
        this.isStringBlank = this.isStringBlank.bind(this);
      }

    }

    return new AppUtils();
  });