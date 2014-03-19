/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

    function readDeclutterConfig() {
        return grunt.file.readJSON('declutter.config.json');
    }

    function readDeclutteredComponentFiles() {
        var declutterConfig = readDeclutterConfig(),
            components = Object.keys(declutterConfig),
            declutteredFiles = [];

        components.forEach(function(component) {
            var componentRules = declutterConfig[component];
            componentRules.forEach(function(rule) {
                declutteredFiles.push(component + '/' + rule);
            });
        });

        return declutteredFiles;
    }

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
                files: [{
                    dot: true,
                    src: [
                        '<%= build.target %>/*'
                    ]
                }]
            },

            bower: {
                files: [{
                    dot: true,
                    src: [
                        '<%= build.source %>/components/**'
                    ]
                }]
            }
        },

        // copy files
        copy: {
            menu: {
                files: [
                    {
                        expand: true,
                        dot: true,
                        cwd: '<%= build.source %>/menu',
                        dest: '<%= build.target %>/menu',
                        src: [
                            '**/*.html',
                            '**/*.js',
                            '**/assets/css/*',
                            '**/assets/images/*',
                            '**/i18n/*.json'
                        ]
                    }
                ]
            },

            pages: {
                files: [
                    {
                        expand: true,
                        dot: true,
                        cwd: '<%= build.source %>/pages',
                        dest: '<%= build.target %>/pages',
                        src: [
                            '**/*.html',
                            '**/*.js',
                            '**/assets/css/*',
                            '**/assets/images/*',
                            '**/i18n/*.json'
                        ]
                    }
                ]
            },

            shared: {
                files: [
                    {
                        expand: true,
                        dot: true,
                        cwd: '<%= build.source %>/shared',
                        dest: '<%= build.target %>/shared',
                        src: [
                            '**/*.js',
                            '**/assets/css/*',
                            '**/assets/images/*'
                        ]
                    }
                ]
            },

            components: {
                files: [
                    {
                        expand: true,
                        dot: true,
                        cwd: '<%= build.source %>/components',
                        dest: '<%= build.target %>/components',
                        src: readDeclutteredComponentFiles()
                    }
                ]
            }
        },

        // watch
        watch: {
            menu: {
                files: [
                    '<%= build.source %>/menu/**/*'
                ],
                tasks: ['jshint', 'copy:menu']
            },

            pages: {
                files: [
                    '<%= build.source %>/pages/**/*'
                ],
                tasks: ['jshint', 'copy:pages']
            },

            shared: {
                files: [
                    '<%= build.source %>/shared/**/*'
                ],
                tasks: ['jshint', 'copy:shared']
            },

            components: {
                files: [
                    '<%= build.source %>/components/**/*'
                ],
                tasks: ['copy:components']
            },

            livereload: {
                options: {
                    livereload: true
                },
                files: [
                    '<%= build.source %>/menu/**/*',
                    '<%= build.source %>/pages/**/*',
                    '<%= build.source %>/shared/**/*'
                ]
            }
        },

        // only use a sub-set of files in Bower components
        declutter: {
            options: {
                rules: readDeclutterConfig()
            },
            files: [
                '<%= build.source %>/components/*'
            ]
        },

        // validate source code with jslint
        jshint: {
            options: {
                reporter: require('jshint-stylish'),
                jshintrc: true
            },
            app: [
                '<%= build.source %>/shared/**/*.js',
                '<%= build.source %>/menu/**/*.js',
                '<%= build.source %>/pages/**/*.js',
                '!<%= build.source %>/**/*.spec.js'
            ],
            tests: [
                '<%= build.source %>/shared/**/*.spec.js',
                '<%= build.source %>/menu/**/*.spec.js',
                '<%= build.source %>/pages/**/*.spec.js'
            ]
        },

        // testing with karma
        karma: {
            options: {
                configFile: 'karma.config.js',
                autoWatch: true
            },

            single: {
                singleRun: true
            },

            continuous: {
                singleRun: false
            }
        }
    });

    grunt.registerTask('default', ['build']);

    grunt.registerTask('build', function (target) {
        grunt.task.run([
            'jshint:app',
            'declutter',
            'clean:target',
            'copy:menu',
            'copy:pages',
            'copy:shared',
            'copy:components'
        ]);
    });

    grunt.registerTask('test', [
        'jshint:tests',
        'test:unit'
    ]);

    grunt.registerTask('test:unit', [
        'jshint:tests',
        'karma:single'
    ]);

    grunt.registerTask('test:unit:loop', [
        'karma:continuous'
    ]);

};