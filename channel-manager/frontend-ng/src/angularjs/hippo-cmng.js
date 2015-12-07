function config ($stateProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/');

  $stateProvider.state('main', {
    url: '/',
    templateUrl: 'hippo-cmng.html'
  });
}

export const hippoCmngModule = angular
  .module('hippo-cmng', [
    'ui.router'
  ])
  .config(config);

angular.element(document).ready(function () {
  angular.bootstrap(document.body, [hippoCmngModule.name], {
    strictDi: true
  });
});
