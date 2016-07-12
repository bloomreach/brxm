/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

module.exports = function gruntTasks(grunt) {
  'use strict';

    // display execution time of each task
  require('time-grunt')(grunt);

    // load all grunt tasks automatically
  require('load-grunt-tasks')(grunt);

    // project configuration
  grunt.initConfig({
    build: require('./build.config.js'),

        // clean target (distribution) folder
    clean: {
      target: {
        src: '<%= build.dashboardtarget %>',
      },
    },

    jshint: {
      files: 'src/main/webapp/js/*.js',
    },

        // copy files to target folder
    copy: {
      components: {
        files: [
          {
            expand: true,
            cwd: '<%= build.npmDir %>',
            dest: '<%= build.dashboardtarget %>',
            src: [
              'angular/angular.js',
              'angular-ui-bootstrap/ui-bootstrap-tpls.js',
              'angular-chosen-localytics/dist/angular-chosen.js',
              'angular-sanitize/angular-sanitize.min.js',
              'angular-tablesort/src/angular-tablesort.js',
              'angular-ui-router/release/angular-ui-router.js',
              'angular-ui-tree/dist/angular-ui-tree.min.css',
              'chosen-npm/public/chosen.jquery.js',
              'google-code-prettify/src/prettify.js',
              'hippo-theme/dist/**',
              'jquery/dist/jquery.js',
              'angular-animate/angular-animate.js',
              'angular-aria/angular-aria.min.js',
            ],
          },
        ],
      },
    },
  });

  grunt.registerTask('build', 'Build everything', [
    'clean:target',
    'jshint',
    'copy:components',
  ]);
};
