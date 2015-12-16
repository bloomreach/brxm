var hippoBuild = require('hippo-build');
var customConfig = require('./build.conf.js');
var cfg = hippoBuild.buildConfig(customConfig);

module.exports = function (config) {
  var options = {};

  options.basePath = '.';
  options.frameworks = ['systemjs', 'jasmine', 'es6-shim'];
  options.files = [
    cfg.src.unitTests
  ];
  options.systemjs = {
    config: {
      transpiler: 'babel',
      defaultJSExtensions: true,
      paths: {
        'babel': cfg.npmDir + 'babel-core/browser.js',
        'systemjs': cfg.npmDir + 'systemjs/dist/system.js',
        'system-polyfills': cfg.npmDir + 'systemjs/dist/system-polyfills.js',
        'es6-module-loader': cfg.npmDir + 'es6-module-loader/dist/es6-module-loader.js',
        'phantomjs-polyfill': cfg.npmDir + 'phantomjs-polyfill/bind-polyfill.js'
      }
    },
    includeFiles: [
      cfg.bowerDir + 'angular/angular.js',
      cfg.bowerDir + 'angular-animate/angular-animate.js',
      cfg.bowerDir + 'angular-aria/angular-aria.js',
      cfg.bowerDir + 'angular-material/angular-material.js',
      cfg.bowerDir + 'angular-ui-router/release/angular-ui-router.js',
      cfg.bowerDir + 'angular-translate/angular-translate.js',
      cfg.bowerDir + 'angular-translate-loader-static-files/angular-translate-loader-static-files.js',
      cfg.bowerDir + 'angular-mocks/angular-mocks.js',
      cfg.npmDir + 'babel-core/external-helpers.js',
      cfg.npmDir + 'systemjs/dist/system-polyfills.js',
      cfg.npmDir + 'systemjs/dist/system-register-only.js',
      cfg.dist.indexScript
    ]
  };
  options.reporters = ['progress', 'coverage'];
  options.coverageReporter = {
    reporters: [
      {
        type: 'html'
      }, {
        type: 'text-summary'
      }
    ]
  };
  options.autoWatch = false;
  options.browsers = ['PhantomJS'];
  options.singleRun = true;
  options.preprocessors = {};
  options.preprocessors[cfg.dist.indexScript] = ['sourcemap', 'coverage'];

  config.set(options);
};
