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
'use strict';

module.exports = function (grunt) {

    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    var fs = require('fs'),
        cfg = {
            file: 'hippo-cms-theme',
            src: 'src',
            dest: '../resources/skin/hippo-cms',
            cp: '../../../target/classes',
            tmp: '.tmp'
    };
    
    grunt.initConfig({
        // Configuration
        cfg: cfg,

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
                files: ['<%= cfg.src %>/**/*.less'],
                tasks: ['less', 'autoprefixer', 'csslint', 'concat', 'clean:tmp']
            },
            livereload: {
                files: ['<%= cfg.dest %>/**'],
                tasks: ['copy:classpath', 'shell:notify']
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '<%= cfg.tmp %>/css/<%= cfg.file %>.css': '<%= cfg.src %>/less/main.less'
                }
            },
            vendors: {
                files: {
                    '<%= cfg.tmp %>/css/open-sans.css': '<%= cfg.src %>/less/lib/open-sans.less',
                    '<%= cfg.tmp %>/css/normalize.css': '<%= cfg.src %>/less/lib/normalize.less'
                }
            }
        },

        // Autoprefix vendor prefixes
        autoprefixer: {
            theme: {
                options: {
                    browsers: ['> 0%']
                },
                src: '<%= cfg.tmp %>/css/<%= cfg.file %>.css',
                dest: '<%= cfg.tmp %>/css/<%= cfg.file %>.css'
            }
        },

        // Lint the css output
        csslint: {
            lessOutput: {
                options: {
                    csslintrc: '.csslintrc'
                },
                src: ['<%= cfg.tmp %>/css/<%= cfg.file %>.css']
            }
        },

        // Minify CSS files
        cssmin: {
            options: {
                report: 'min'
            },
            theme: {
                files: {
                    '<%= cfg.dest %>/css/<%= cfg.file %>.min.css': ['<%= cfg.dest %>/css/<%= cfg.file %>.css']
                }
            }
        },

        // Concat files
        concat: {
            options: {
                stripBanners: true
            },
            css: {
                src: [
                    '<%= cfg.tmp %>/css/open-sans.css', 
                    '<%= cfg.tmp %>/css/normalize.css', 
                    '<%= cfg.tmp %>/css/<%= cfg.file %>.css'
                ],
                dest: '<%= cfg.dest %>/css/<%= cfg.file %>.css'
            }
        },

        copy: {
            fonts: {
                files: [
                    {
                        expand: true,
                        cwd: 'src/fonts',
                        src: ['**/*.{otf,eot,svg,ttf,woff}'],
                        dest: '<%= cfg.dest %>/fonts/'
                    }
                ]
            },

            classpath: { // Copy resources to classpath so Wicket will pick them up 
                expand: true,
                cwd: '<%= cfg.dest %>',
                src: ['**'],
                dest: '<%= cfg.cp %>/skin/hippo-cms/',
                filter: function() {
                    //little hack to force it to only copy when dest exists
                    return fs.existsSync(cfg.cp + '/skin/hippo-cms');
                }
            }
        },

        // clean destination folder and bower components
        clean: {
            tmp: {
                files: [
                    {src: [ '<%= cfg.tmp %>/**' ], nonull: true}
                ]
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
                command: "command -v terminal-notifier >/dev/null 2>&1 && terminal-notifier -group 'Hippo CMS' -title 'Grunt build' -subtitle 'Finished' -message 'LiveReloading'"
            }
        }
    });

    grunt.registerTask('default', ['build', 'watch']);

    // build theme
    grunt.registerTask('build', 'Build the theme', [
        'less',
        'autoprefixer',
        'csslint',
        'concat',
        'cssmin:theme',
        'copy:fonts',
        'clean:tmp'
    ]);

    // install
    grunt.registerTask('install:theme', 'Build and install the theme', [
        'build',
        'copy:classpath'
    ]);
    
};
