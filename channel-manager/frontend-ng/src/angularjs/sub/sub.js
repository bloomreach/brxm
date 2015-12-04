import { SubCtrl } from './sub.controller.js';

function config ($stateProvider) {
  $stateProvider.state('main.sub', {
    url: 'sub/',
    templateUrl: 'sub/sub.html',
    controller: 'SubCtrl',
    controllerAs: 'sub'
  });
}

export const subModule = angular
  .module('sub', [])
  .config(config)
  .controller('SubCtrl', SubCtrl);
