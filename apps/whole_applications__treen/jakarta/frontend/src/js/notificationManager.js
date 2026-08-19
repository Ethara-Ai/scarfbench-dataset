define([
  'knockout'
],
  function (ko) {
    'use strict';

    class NotificationManager {

      addNotification(notification) {
        this.notifications.push(notification);
      }

      removeAllNotificationsOfType(notificationType) {
        this.notifications.remove(item => item.type === notificationType);
      }

      constructor() {
        this.notifications = ko.observableArray([]);
        this.addNotification = this.addNotification.bind(this);
        this.removeAllNotificationsOfType = this.removeAllNotificationsOfType.bind(this);
      }

    }

    return new NotificationManager();
  });
