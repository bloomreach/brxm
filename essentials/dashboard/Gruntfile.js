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

module.exports = function (grunt) {
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
                src: '<%= build.dashboardtarget %>'
            }
        },

        // copy files to target folder
        copy: {
            components: {
                files: [
                    {
                        expand: true,
                        cwd: '<%= build.bower %>',
                        dest: '<%= build.dashboardtarget %>',
                        src: [
                            'angular/**',
                            'angular-animate/**',
                            'angular-bootstrap/**',
                            'angular-chosen-localytics/**',
                            'angular-ui-router/**',
                            'angular-ui-tree/**',
                            'bootstrap/**',
                            'chosen/**',
                            'es5-shim/**',
                            'hippo-theme/**',
                            'jquery/**'
                        ]
                    }
                ]
            }
        }
    });

    grunt.registerTask('build', 'Build everything', [
        'clean:target',
        'copy:components'
    ]);
};