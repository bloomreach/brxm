(function () {
  'use strict';

  function MainService () {
    var main = this;
    main.message = 'Awesome Mainservice message';
  }

  angular
    .module('hippo.ngbp')
    .service('MainService', MainService);
})();