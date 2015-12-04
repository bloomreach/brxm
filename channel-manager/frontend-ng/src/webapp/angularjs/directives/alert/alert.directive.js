(function () {
  'use strict';

  function alertDirective ($rootScope) {
    return {
      restrict: 'E',
      templateUrl: 'angularjs/directives/alert/alert.directive.html',
      scope: {
        message: '='
      },
      link: function () {}
    };
  }

  angular
    .module('hippo.ngbp')
    .directive('alert', alertDirective);
})();
