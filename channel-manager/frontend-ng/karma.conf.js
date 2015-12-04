// Karma configuration

var hippoBuild = require('hippo-build');
var customConfig = require('./build.conf.js');
var cfg = hippoBuild.buildConfig(customConfig);

module.exports = function (config) {
  var options = {
    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '.',

    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      {pattern: cfg.srcDir + '**/*', included: false, served: true},
      {pattern: cfg.bowerDir + '**/*', included: false, served: true},
      cfg.bowerDir + 'angular/angular.js',
      cfg.bowerDir + 'angular-ui-router/release/angular-ui-router.js',
      cfg.bowerDir + 'angular-mocks/angular-mocks.js',
      cfg.dist.indexScript,
      cfg.src.unitTests
    ],

    preprocessors: {},

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress'],

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,

    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true
  };

  options.preprocessors[cfg.dist.indexScript] = ['sourcemap'];

  config.set(options);
};
