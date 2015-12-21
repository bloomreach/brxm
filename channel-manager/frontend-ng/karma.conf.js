/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var hippoBuild = require('hippo-build');
var customConfig = require('./build.conf.js');
var cfg = hippoBuild.buildConfig(customConfig);

module.exports = function(config) {
  var options = {};

  options.basePath = '.';

  options.frameworks = ['systemjs', 'jasmine', 'es6-shim'];
  options.reporters = ['progress', 'coverage'];
  options.preprocessors = {};

  options.preprocessors[cfg.src.scripts] = ['coverage'];
  options.coverageReporter = {
    instrumenters: {
      isparta: require('isparta')
    },
    instrumenter: {
      '**/*.js': 'isparta'
    },
    reporters: [{
      type: 'html'
    }, {
      type: 'text-summary'
    }]
  };

  options.preprocessors[cfg.src.templates] = ['ng-html2js'];
  options.ngHtml2JsPreprocessor = {
    stripPrefix: 'src/angularjs/',
    moduleName: cfg.projectName + '-templates'
  };

  options.files = [
    cfg.src.templates,
    cfg.src.scripts,
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
      cfg.npmDir + 'systemjs/dist/system-register-only.js'
    ]
  };

  options.browsers = ['PhantomJS'];
  options.autoWatch = false;
  options.singleRun = true;

  config.set(options);
};
