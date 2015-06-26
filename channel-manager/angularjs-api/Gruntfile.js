/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)

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

'use strict';

module.exports = function (grunt) {
  /*
   * Load grunt tasks that are in the package.json
   */
  require('load-grunt-tasks')(grunt);

  /*
   * Log execution time of grunt tasks
   */
  require('time-grunt')(grunt);

  /*
   * Initiate grunt config
   */
  grunt.initConfig({

    /*
     * Load in our build configuration file.
     */
    buildConfig: require('./build.config.js'),

    watch: {
      options: {
        interrupt: true,
        livereloadOnError: false
      },

      /*
       * When these config/build files change, we want to run related tasks.
       * When your Gruntfile changes, it will automatically be reloaded!
       */
      jshintrc: {
        files: ['.jshintrc'],
        tasks: ['jshint']
      },

      karmaConf: {
        files: ['karma.conf.js'],
        tasks: ['karma:continuous:run']
      },

      gruntfile: {
        options: {
          reload: true
        },
        files: ['Gruntfile.js', 'build.config.js'],
        tasks: ['jshint:gruntfile']
      },

      /*
       * When our JavaScript source files change, we want to lint them,
       * run our unit tests and provide dist files.
       */
      jssrc: {
        files: ['<%= buildConfig.js %>'],
        tasks: ['jshint:src', 'karma:continuous:run', 'concat:js', 'uglify:dist']
      },

      /*
       * When a JavaScript unit test file changes, we only want to lint it and
       * run the unit tests.
       */
      jsunit: {
        files: ['<%= buildConfig.unit %>'],
        tasks: ['jshint:unit', 'karma:continuous:run']
      },

      /*
       * When our templates change, we only rewrite the template cache.
       */
      tpls: {
        files: ['<%= buildConfig.tpl %>'],
        tasks: ['html2js', 'concat:js', 'uglify:dist']
      },

      /*
       * Execute a livereload when these files change.
       */
      livereload: {
        options: {
          livereload: true
        },
        files: [
          '<%= buildConfig.dist_dir %>/**',
          '!<%= buildConfig.dist_dir %>/**/*.*.map',
          '<%= buildConfig.docs_dir %>/**'
        ]
      }
    },

    /*
     * `jshint` defines the rules of our linter as well as which files we
     * should check. This file, all javascript sources, and all our unit tests
     * are linted based on the policies listed in `.jshintrc`.
     */
    jshint: {
      options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
      },
      gruntfile: ['Gruntfile.js'],
      src: ['<%= buildConfig.js %>'],
      unit: ['<%= buildConfig.unit %>']
    },

    /*
     * The concat task concatenates the source files in the given order
     * (or alphabetically if its a glob pattern) to the provided destination file.
     */
    concat: {
      options: {
        sourceMap: true
      },
      js: {
        src: [
          '<%= buildConfig.mainjs %>',
          '<%= buildConfig.jstpl %>',
          '<%= buildConfig.js %>'
        ],
        dest: '<%= buildConfig.dist_dir %>/js/main.js'
      }
    },

    /*
     * Uglify minifies the provides js files.
     */
    uglify: {
      options: {
        preserveComments: 'some'
      },
      dist: {
        files: {
          '<%= buildConfig.dist_dir %>/js/main.min.js': ['<%= buildConfig.dist_dir %>/js/main.js']
        }
      }
    },

    /*
     * HTML2JS is a Grunt plugin that takes all of your template files and
     * places them into JavaScript files as strings that are added to
     * AngularJS's template cache.
     */
    html2js: {
      src: {
        options: {
          module: '<%= buildConfig.jstplModule %>',
          base: '<%= buildConfig.jstplModuleBase %>',
          useStrict: true,
          htmlmin: {
            collapseWhitespace: true,
            collapseBooleanAttributes: true,
            removeComments: true
          }
        },
        src: ['<%= buildConfig.tpl %>'],
        dest: '<%= buildConfig.jstpl %>'
      }
    },

    /*
     * The directories/files to delete when `grunt clean` is executed.
     */
    clean: {
      tmp: '<%= buildConfig.tmp_dir %>',
      dist: '<%= buildConfig.dist_dir %>',
      docs: '<%= buildConfig.docs_dir %>'
    },

    /*
     * Karma test server for unit testing the source code.
     */
    karma: {
      options: {
        configFile: 'karma.conf.js'
      },
      continuous: {
        singleRun: false,
        background: true
      },
      single: {
        singleRun: true
      }
    },

    /*
     * NGdocs automaticly generates documentation on the angular source code.
     */
    ngdocs: {
      options: {
        dest: '<%= buildConfig.docs_dir %>',
        scripts: ['<%= buildConfig.components_dir %>/angular/angular.js'],
        title: 'API Docs'
      },

      api: {
        title: 'API reference',
        src: ['<%= buildConfig.js %>']
      }
    },

    /*
     * Connect sets up a server to view the application in.
     * This can be used to develop the application or
     * to view the distribution version of the application.
     */
    connect: {
      docs: {
        options: {
          livereload: true,
          base: ['.'],
          port: 9000,
          hostname: '0.0.0.0',
          open: 'http://localhost:9000/target/docs/#/'
        }
      }
    }
  });

  /*
   * Register extra grunt tasks.
   */
  grunt.registerTask('default', [
    'build',
    'ngdocs',
    'karma:continuous:start',
    'watch'
  ]);

  grunt.registerTask('build', 'Build the distribution', [
    'clean',
    'html2js',
    'jshint',
    'concat',
    'uglify'
  ]);

  grunt.registerTask('server', 'Build, test, and show the docs continuously', [
    'build',
    'ngdocs',
    'connect',
    'karma:continuous:start',
    'watch'
  ]);

  grunt.registerTask('test', 'Run unit tests', [
    'html2js',
    'karma:single'
  ]);
};
