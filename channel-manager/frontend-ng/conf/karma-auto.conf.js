const conf = require('./gulp.conf');
const karmaFixtureProxyPath = '/base/src/angularjs/';

module.exports = function karmaConfig(config) {
  const configuration = {
    basePath: '../',
    singleRun: false,
    autoWatch: true,
    logLevel: 'INFO',
    browsers: [
      'Chrome',
    ],
    frameworks: [
      'jasmine-jquery', 'jasmine',
    ],
    files: [
      'node_modules/es6-shim/es6-shim.js',
      'node_modules/dragula/dist/dragula.min.js',
      'node_modules/dragula/dist/dragula.min.css',
      conf.path.src('index.spec.js'),
      {
        pattern: conf.path.src('**/*.fixture.+(js|html|css|json)'),
        included: false,
      },
      'node_modules/jquery/dist/jquery.js',
    ],
    preprocessors: {
      [conf.path.src('index.spec.js')]: [
        'webpack',
        'sourcemap',
      ],
      [conf.path.src('**/!(*fixture).html')]: [
        'ng-html2js',
      ],
    },
    proxies: {
      '/spec/javascripts/fixtures/': karmaFixtureProxyPath,
      '/spec/javascripts/fixtures/json/': karmaFixtureProxyPath,
      '/styles/dragula.min.css': '/base/node_modules/dragula/dist/dragula.min.css',
      '/scripts/dragula.min.js': '/base/node_modules/dragula/dist/dragula.min.js',
    },
    ngHtml2JsPreprocessor: {
      stripPrefix: `${conf.paths.src}/`,
    },
    reporters: ['mocha', 'coverage'],
    coverageReporter: {
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
      ],
    },
    mochaReporter: {
      output: 'minimal',
    },
    webpack: require('./webpack-test.conf'),
    webpackMiddleware: {
      noInfo: true,
    },
    plugins: [
      require('karma-chrome-launcher'),
      require('karma-coverage'),
      require('karma-jasmine'),
      require('karma-jasmine-jquery'),
      require('karma-mocha-reporter'),
      require('karma-ng-html2js-preprocessor'),
      require('karma-sourcemap-loader'),
      require('karma-webpack'),
    ],
  };

  config.set(configuration);
};
