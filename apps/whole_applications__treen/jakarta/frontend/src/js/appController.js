define([
  'ojs/ojcontext',
  'ojs/ojmodule-element-utils',
  'ojs/ojresponsiveutils',
  'ojs/ojresponsiveknockoututils',
  'knockout',
  'loginManager',
  'notificationManager',
  'notebookManager',
  'ojs/ojarraydataprovider',
  'ojs/ojknockout',
  'ojs/ojmessages'
],
  function (
    Context,
    ModuleElementUtils,
    ResponsiveUtils,
    ResponsiveKnockoutUtils,
    ko,
    loginManager,
    notificationManager,
    notebookManager,
    ArrayDataProvider) {
    'use strict';

    class ControllerViewModel {

      constructor() {
        // Media queries for repsonsive layouts
        const smQuery = ResponsiveUtils.getFrameworkQuery(ResponsiveUtils.FRAMEWORK_QUERY_KEY.SM_ONLY);
        this.smScreen = ResponsiveKnockoutUtils.createMediaQueryObservable(smQuery);

        // User Info used in Global Navigation area
        this.userLogin = loginManager.userLogin;
        // Action menu in Global Navigation area
        this.userMenuAction = (event) => {
          if (event.detail.selectedValue === 'logout') {
            notificationManager.removeAllNotificationsOfType('login');
            loginManager
              .logout()
              .fail((jqXHR, textStatus, errorThrown) => {
                notificationManager.addNotification({
                  severity: 'error',
                  summary: 'Logout failed',
                  detail: `${textStatus} - ${errorThrown}`,
                  type: 'login'
                });
              });
            return;
          }
          if (event.detail.selectedValue === 'reload') {
            notebookManager.init()
              .fail((jqXHR, textStatus, errorThrown) => {
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
                    summary: 'Failed to reload the notebook',
                    detail: serverMessage ? serverMessage : `${textStatus} - ${errorThrown}`,
                    type: 'note'
                  });
                }
              });
          }
        };
        this.ModuleElementUtils = ModuleElementUtils;
        this.dataProvider = new ArrayDataProvider(notificationManager.notifications);
      }

    }

    loginManager.init();
    // release the application bootstrap busy state
    Context.getPageContext().getBusyContext().applicationBootstrapComplete();
    return new ControllerViewModel();
  }
);
