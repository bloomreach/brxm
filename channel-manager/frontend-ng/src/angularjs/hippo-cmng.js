function config ($stateProvider, $urlRouterProvider, $translateProvider) {
  $urlRouterProvider.otherwise('/');

  $stateProvider.state('main', {
    url: '/',
    templateUrl: 'hippo-cmng.html',
    resolve: {
      translations: function ($translate) {
        // TODO use locale from "config service" rather than hard-coded.
        return $translate.use('en')
          .catch(function () {
            $translate.use($translate.fallbackLanguage());
          });
      }
    }
  });

  // translations
  $translateProvider.useStaticFilesLoader({
    prefix: 'i18n/hippo-cmng.',
    suffix: '.json'
  });
  $translateProvider.fallbackLanguage('en');
  $translateProvider.useSanitizeValueStrategy('sanitize');
}

export const hippoCmngModule = angular
  .module('hippo-cmng', [
    'pascalprecht.translate',
    'ui.router'
  ])
  .config(config);

angular.element(document).ready(function () {
  angular.bootstrap(document.body, [hippoCmngModule.name], {
    strictDi: true
  });
});
