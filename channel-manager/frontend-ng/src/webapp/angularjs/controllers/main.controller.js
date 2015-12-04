(function () {
  'use strict';

  function MainCtrl (MainService) {
    var main = this;
    main.message = MainService.message;
  }

  angular
    .module('hippo.ngbp')
    .controller('MainCtrl', MainCtrl);
})();
