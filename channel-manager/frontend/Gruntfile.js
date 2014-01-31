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
            app: {
                files: [
                    {
                        expand: true,
                        dot: true,
                        cwd: '<%= build.source %>/app',
                        dest: '<%= build.target %>/app',
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
            app: {
                files: [
                    '<%= build.source %>/app/**/*'
                ],
                tasks: ['copy:app']
            },

            components: {
                files: [
                    '<%= build.source %>/components'
                ],
                tasks: ['copy:components']
            },

            livereload: {
                options: {
                    livereload: true
                },
                files: [
                    '<%= build.source %>/app/**/*'
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
        jslint: {
            app: {
                src: [
                    '<%= build.source %>/app/**/*.js'
                ],
                exclude: [
                    '<%= build.source %>/app/**/*.spec.js'
                ],
                directives: {
                    predef: [
                        '_',
                        'angular'
                    ],
                    browser: true,
                    nomen: true,
                    todo: true,
                    white: true
                }
            },
            tests: {
                src: [
                    '<%= build.source %>/app/**/*.spec.js'
                ],
                directives: {
                    predef: [
                        'after',
                        'afterEach',
                        'before',
                        'beforeEach',
                        'describe',
                        'expect',
                        'inject',
                        'jasmine',
                        'module',
                        'it',
                        'spyOn'
                    ],
                    white: true
                }
            }
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
            'jslint',
            'test:unit',
            'declutter',
            'clean:target',
            'copy:app',
            'copy:components'
        ]);
    });

    grunt.registerTask('test', [
        'test:unit'
    ]);

    grunt.registerTask('test:unit', [
        'karma:single'
    ]);

    grunt.registerTask('test:unit:loop', [
        'karma:continuous'
    ]);

};