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
            livereload: {
                files: ['<%= build.skin %>/**/*.css'],
                tasks: ['newer:copy:sources2classpath', 'shell:notify']
            },
            images: {
                files: ['<%= build.src %>/images/**'],
                tasks: ['newer:copy:binaries', 'svgmin:theme', 'svgstore:theme', 'newer:copy:images2classpath']
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '<%= build.tmp %>/css/<%= build.file %>.css': '<%= build.src %>/less/main.less'
                }
            },
            vendors: {
                files: {
                    '<%= build.tmp %>/css/open-sans.css':    '<%= build.src %>/less/lib/open-sans.less',
                    '<%= build.tmp %>/css/wicket.css':       '<%= build.src %>/less/lib/wicket.less',
                    '<%= build.tmp %>/css/style-test.css':   '<%= build.src %>/less/lib/style-test.less'
                }
            }
        },

        // Autoprefix vendor prefixes
        autoprefixer: {
            theme: {
                options: {
                    browsers: ['> 0%']
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
                    { removeViewBox: false },
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

        svgstore: {
            options: {
                prefix : 'hi-',
                svg: {
                    xmlns: 'http://www.w3.org/2000/svg',
                    class: 'hi-defs'
                }
            },
            theme: {
                files: {
                    '<%= build.images %>/icons/hippo-icons.svg': ['<%= build.images %>/**/*.svg']
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
                    '<%= build.tmp %>/css/open-sans.css',
                    '<%= build.bower %>/normalize.css/normalize.css',
                    '<%= build.bower %>/jquery-selectric/dist/selectric.css',
                    '<%= build.tmp %>/css/style-test.css',
                    '<%= build.tmp %>/css/<%= build.file %>.css',
                    '<%= build.tmp %>/css/wicket.css'
                ],
                dest: '<%= build.skin %>/css/<%= build.file %>.css'
            },
            js: {
                src: [
                    '<%= build.bower %>/jquery-selectric/dist/jquery.selectric.js'
                ],
                dest: '<%= build.skin %>/js/<%= build.file %>.js'
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
                    }
                ]
            },

            sources2classpath: {
                // Copy resources to classpath so Wicket will pick them up
                expand: true,
                cwd: '<%= build.skin %>',
                src: '**/*',
                dest: '<%= build.target %>/classes/skin/hippo-cms',
                filter: function () {
                    //little hack to force it to only copy when dest exists
                    return classPathExists();
                }
            },

            images2classpath: {
                // Copy images to the classpath so Wicket will pick them up
                expand: true,
                cwd: '<%= build.images %>',
                src: '**/*',
                dest: '<%= build.target %>/classes/org/hippoecm/frontend/skin/images',
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

    grunt.registerTask('default', ['install', 'watch']);

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
        'svgmin:theme',
        'svgstore:theme',
        'clean:tmp'
    ]);

    // install
    grunt.registerTask('install', 'Build and install the theme', [
        'build',
        'newer:copy:sources2classpath',
        'newer:copy:images2classpath'
    ]);

};
