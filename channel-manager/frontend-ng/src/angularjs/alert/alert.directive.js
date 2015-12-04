export function alertDirective ($rootScope) {
  "ngInject";
  return {
    restrict: 'E',
    templateUrl: 'alert/alert.directive.html',
    scope: {
      message: '='
    },
    link: function () {}
  };
}
