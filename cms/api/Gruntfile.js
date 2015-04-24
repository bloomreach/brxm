/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

    grunt.initConfig({

        build: buildConfig,

        // Watch for file changes and run corresponding tasks
        watch: {
            options: {
                livereload: true,
                interrupt: false,
                livereloadOnError: false
            },
            gruntfile: {
                files: ['Gruntfile.js']
            },
            less: {
                options: {
                    livereload: false
                },
                files: ['<%= build.src %>/**/*.less'],
                tasks: ['less', 'autoprefixer', 'csslint', 'concat', 'clean:tmp']
            },
            reloadCompiledCss: {
                files: ['<%= build.skin %>/**/*.css']
            },
            extjsLess: {
                options: {
                    livereload: false
                },
                files: ['<%= build.extjs.src %>'],
                tasks: ['less:extjs']
            },
            reloadCompiledExtJsCss: {
                files: ['<%= build.extjs.target %>']
            },
            images: {
                files: ['<%= build.src %>/images/**'],
                tasks: ['newer:copy:binaries', 'svgmin:theme']
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '<%= build.tmp %>/css/<%= build.file %>.css': '<%= build.src %>/less/main.less'
                }
            },
            nolint: {
                files: {
                    '<%= build.tmp %>/css/wicket.css':       '<%= build.src %>/less/nolint/wicket.less',
                    '<%= build.tmp %>/css/workarounds.css':  '<%= build.src %>/less/nolint/workarounds.less'
                }
            },
            vendors: {
                files: {
                    '<%= build.tmp %>/css/open-sans.css':    '<%= build.src %>/less/lib/open-sans.less'
                }
            },
            extjs: {
                files: {
                    '<%= build.extjs.target %>': '<%= build.extjs.src %>'
                }
            }
        },

        // Autoprefix vendor prefixes
        autoprefixer: {
            theme: {
                options: {
                    browsers: ['last 1 Chrome versions', 'last 1 Firefox versions', 'Safari >= 7', 'Explorer >= 10']
                },
                src: '<%= build.tmp %>/css/<%= build.file %>.css',
                dest: '<%= build.tmp %>/css/<%= build.file %>.css'
            }
        },

        // Lint the css output
        csslint: {
            lessOutput: {
                options: {
                    csslintrc: '.csslintrc'
                },
                src: ['<%= build.tmp %>/css/<%= build.file %>.css']
            }
        },

        // Minify CSS files
        cssmin: {
            options: {
                report: 'min'
            },
            theme: {
                files: {
                    '<%= build.skin %>/css/<%= build.file %>.min.css': ['<%= build.skin %>/css/<%= build.file %>.css']
                }
            }
        },

        svgmin: {
            options: {
                plugins: [
                    { cleanupIDs: false },
                    { removeUselessStrokeAndFill: true }
                ]
            },
            theme: {
                expand: true,
                cwd: '<%= build.images %>/',
                src: ['**/*.svg'],
                dest: '<%= build.images %>'
            }
        },

        // Concat files
        concat: {
            options: {
                stripBanners: true
            },
            css: {
                src: [
                    '<%= build.tmp %>/css/open-sans.csssss',
                    '<%= build.bower %>/normalize.css/normalize.css',
                    '<%= build.bower %>/jquery-selectric/public/selectric.css',
                    '<%= build.tmp %>/css/<%= build.file %>.css',
                    '<%= build.tmp %>/css/wicket.css',
                    '<%= build.tmp %>/css/workarounds.css'
                ],
                dest: '<%= build.skin %>/css/<%= build.file %>.css',
                nonull: true
            },
            js: {
                src: [
                    '<%= build.bower %>/jquery-selectric/public/jquery.selectric.js'
                ],
                dest: '<%= build.skin %>/js/<%= build.file %>.js',
                nonull: true
            }
        },

        uglify: {
            dist: {
                files: {
                    '<%= build.skin %>/js/<%= build.file %>.min.js': ['<%= concat.js.dest %>']
                }
            }
        },

        copy: {
            binaries: {
                files: [
                    {
                        expand: true,
                        cwd: '<%= build.bower %>/open-sans-fontface/fonts',
                        src: ['**/*.{otf,eot,svg,ttf,woff}'],
                        dest: '<%= build.skin %>/fonts/open-sans/'
                    },
                    {
                        // images go into the package relative to Icons.java
                        expand: true,
                        nonull: true,
                        cwd: '<%= build.src %>/images',
                        src: ['**/*'],
                        dest: '<%= build.images %>'
                    },
                    {
                      // Copy the svg sprite from the hippo theme to the correct location
                      // so it can be used in the java code.
                      expand: true,
                      nonull: true,
                      cwd: '<%= build.bower %>/hippo-theme/dist/images/',
                      src: ['hippo-icon-sprite.svg'],
                      dest: '<%= build.images %>/icons/'
                    }
                ]
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

            // clean up copied image, font and css files
            copies: {
                src: ['<%= build.images %>', '<%= build.skin %>']
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
                command: "command -v terminal-notifier >/dev/null 2>&1 && terminal-notifier -group 'Hippo CMS' -title 'Grunt build' -subtitle 'Finished' -message 'LiveReloading' || echo 'done'"
            }
        }
    });

    grunt.registerTask('default', ['build', 'watch']);

    // build theme
    grunt.registerTask('build', 'Build the theme', [
        'clean:copies',
        'less',
        'autoprefixer',
        'csslint',
        'concat',
        'uglify',
        'cssmin:theme',
        'copy:binaries',
    ]);

};
