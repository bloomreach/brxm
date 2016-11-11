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
'use strict';

module.exports = function (grunt) {

    // load all grunt tasks automatically
    require('load-grunt-tasks')(grunt);

    // display execution time of each task
    require('time-grunt')(grunt);

    grunt.initConfig({

        build : require('./build.config.js'),

        copy: {
            binaries: {
                files: [
                    {
                        expand: true,
                        cwd: '<%= build.npmDir %>/font-awesome/fonts',
                        src: ['**/*.{otf,eot,svg,ttf,woff,woff2}'],
                        dest: '<%= build.faresources %>/fonts',
                    },
                    {
                        expand: true,
                        cwd: '<%= build.npmDir %>/font-awesome/css',
                        src: ['**/*.{css,map}'],
                        dest: '<%= build.faresources %>/css',
                    }
                ]
            }
        },

        clean: {
            copies: {
                src: '<%= build.faresources %>'
            }
        },
    });

    grunt.registerTask('default', 'build');

    // build theme
    grunt.registerTask('build', 'Build the theme', [
        'clean:copies',
        'copy:binaries',
    ]);
};
