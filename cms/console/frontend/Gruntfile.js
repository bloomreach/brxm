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
'use strict';

var fs = require('fs');

module.exports = function (grunt) {

    // load all grunt tasks automatically
    require('load-grunt-tasks')(grunt);

    // display execution time of each task
    require('time-grunt')(grunt);

    var buildConfig = require('./build.config.js');
    function classPathExists() {
        return fs.existsSync(buildConfig.target + '/classes/');
    }

    grunt.initConfig({

        build: buildConfig,

        copy: {
            binaries: {
                files: [
                    {
                        expand: true,
                        cwd: '<%= build.bower %>/fontawesome/fonts',
                        src: ['**/*.{otf,eot,svg,ttf,woff,woff2}'],
                        dest: '<%= build.faresources %>/fonts'
                    },
                    {
                        expand: true,
                        cwd: '<%= build.bower %>/fontawesome/css',
                        src: ['**/*.{css,map}'],
                        dest: '<%= build.faresources %>/css'
                    }
                ]
            },

            sources2classpath: {
                // Copy resources to classpath so Wicket will pick them up
                expand: true,
                cwd: '<%= build.faresources %>',
                src: '**/*',
                dest: '<%= build.target %>/classes/skin/hippo-console',
                filter: function () {
                    //little hack to force it to only copy when dest exists
                    return classPathExists();
                }
            }
        },

        clean: {
            // clean tmp folder
            tmp: {
                src: '<%= build.tmp %>'
            },

            // clean bower components
            bower: {
                src: '<%= build.bower %>'
            },

            // clean up copied font and css files
            copies: {
                src: '<%= build.faresources %>'
            }
        },

        // Execute shell commands
        shell: {
            options: {
                stdout: true,
                stderr: true
            },

            // Notify user when reloading. Currently only works on OSX with terminal-notifier installed (brew install terminal-notifier)
            notify: {
                command: "command -v terminal-notifier >/dev/null 2>&1 && terminal-notifier -group 'Hippo Console' -title 'Grunt build' -subtitle 'Finished' -message 'LiveReloading' || echo 'done'"
            }
        }
    });

    grunt.registerTask('default', 'install');

    // build theme
    grunt.registerTask('build', 'Build the theme', [
        'clean:copies',
        'copy:binaries',
        'clean:tmp'
    ]);

    // install
    grunt.registerTask('install', 'Build and install the theme', [
        'build',
        'newer:copy:sources2classpath'
    ]);

};
