(function () {
  'use strict';

  function config ($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise('/');

    $stateProvider.state('main', {
      url: '/',
      templateUrl: 'angularjs/hippo.ngbp.html',
      controller: 'MainCtrl as main'
    });
  }

  angular
    .module('hippo.ngbp', [
      'ui.router',
      'hippo.ngbp.templates',
      'hippo.ngbp.api',
      'sub'
    ])
    .config(config);

  angular.element(document).ready(function () {
    angular.bootstrap(document.body, ['hippo.ngbp'], {
      strictDi: true
    });
  });
})();
