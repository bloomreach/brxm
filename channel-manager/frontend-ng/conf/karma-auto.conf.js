const conf = require('./gulp.conf');

module.exports = function karmaConfig(config) {
  const configuration = {
    basePath: '../',
    singleRun: false,
    autoWatch: true,
    logLevel: 'INFO',
    junitReporter: {
      outputDir: 'test-reports',
    },
    browsers: [
      'Chrome',
    ],
    frameworks: [
      'jasmine',
    ],
    files: [
      'node_modules/es6-shim/es6-shim.js',
      conf.path.src('index.spec.js'),
      conf.path.src('**/*.html'),
    ],
    preprocessors: {
      [conf.path.src('index.spec.js')]: [
        'webpack',
        'sourcemap',
      ],
      [conf.path.src('**/*.html')]: [
        'ng-html2js',
      ],
    },
    ngHtml2JsPreprocessor: {
      stripPrefix: `${conf.paths.src}/`,
    },
    reporters: ['progress', 'coverage'],
    coverageReporter: {
      type: 'html',
      dir: 'coverage/',
    },
    webpack: require('./webpack-test.conf'),
    webpackMiddleware: {
      noInfo: true,
    },
    plugins: [
      require('karma-jasmine'),
      require('karma-junit-reporter'),
      require('karma-coverage'),
      require('karma-chrome-launcher'),
      require('karma-ng-html2js-preprocessor'),
      require('karma-webpack'),
    ],
  };

  config.set(configuration);
};
