(function () {
  'use strict';

  function config ($stateProvider) {
    $stateProvider.state('main.sub', {
      url: 'sub/',
      templateUrl: 'angularjs/components/sub/sub.html',
      controller: 'SubCtrl as sub'
    });
  }

  angular
    .module('sub', [])
    .config(config);
})();
