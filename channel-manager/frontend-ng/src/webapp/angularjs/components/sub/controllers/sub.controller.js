(function () {
  'use strict';

  function SubCtrl () {
    this.message = 'Awesome sub module';
  }

  angular
    .module('hippo.ngbp')
    .controller('SubCtrl', SubCtrl);
})();