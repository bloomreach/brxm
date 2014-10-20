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
                tasks: ['less', 'autoprefixer', 'csslint', 'concat:css', 'clean:tmp']
            },
            js: {
                files: ['<%= cfg.src %>/**/*.js', '!**/*.spec.js'],
                tasks: ['concat:js', 'uglify:js']
            },
            images: {
                files: ['<%= cfg.src %>/images/**/*.{png,jpg,gif}'],
                tasks: ['newer:imagemin']
            },
            livereload: {
                files: ['<%= cfg.dest %>/**/*.css'],
                tasks: ['copy:theme', 'copy:classpath', 'shell:notify']
            }
        },

        // clean destination folder and bower components
        clean: {
            bower: [ 'components/**' ],
            tmp: [ '<%= cfg.tmp %>/**' ]
        },

        // Check if JS files are according to conventions specified in .jshintrc
        jshint: {
            options: {
                'jshintrc': true,
                reporter: require('jshint-stylish')
            },
            all: ['<%= cfg.src %>/**/*.js', '!**/*.spec.js']
        },

        // Concat files
        concat: {
            options: {
                stripBanners: true
            },
            css: {
                src: ['<%= cfg.tmp %>/css/**/*.css'],
                dest: '<%= cfg.dest %>/css/<%= cfg.file %>.css'
            }
        },

        // Minify JS files
        uglify: {
            options: {
                preserveComments: 'some'
            },
            theme: {
                files: {
                    '<%= cfg.dest %>/js/<%= cfg.file %>.min.js': ['<%= cfg.dest %>/js/<%= cfg.file %>.js']
                }
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '<%= cfg.tmp %>/css/<%= cfg.file %>.css': '<%= cfg.src %>/less/main.less'
                }
            }
/*
            , vendors: {
                files: {
                    '<%= cfg.tmp %>/css/open-sans.css': '<%= cfg.src %>/less/open-sans.less'
                }
            }
*/
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

        // Minify images
        imagemin: {
            src: {
                files: [{
                    expand: true,
                    cwd: '<%= cfg.src %>/images/',
                    src: ['**/*.{png,jpg,gif,svg}'],
                    dest: '<%= cfg.src %>/images/'
                }]
            }
        },

        copy: {
            theme: {
                files: [
                    {
                        expand: true,
                        cwd: 'src/images',
                        src: ['**/*.{png,jpg,gif}'],
                        dest: '<%= cfg.dest %>/images/'
                    },
                    {
                        expand: true,
                        cwd: 'src/fonts',
                        src: ['**/*.{css,otf,eot,svg,ttf,woff}'],
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
    grunt.registerTask('build:theme', 'Build the theme', [
        'jshint',
        'less',
        'autoprefixer',
        'csslint',
        'imagemin',
        'copy:theme',
        'concat',
        'uglify:theme',
        'cssmin:theme',
        'copy:classpath',
        'clean:tmp'
    ]);
    
    
};
