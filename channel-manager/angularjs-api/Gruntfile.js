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

    var userhome = require('userhome');

    // display execution time of each task
    require('time-grunt')(grunt);

    // load all grunt tasks automatically
    require('load-grunt-tasks')(grunt);

    var cfg = {
        tmpDir: '.tmp'
    };

    cfg.tmpRepoDir = userhome(cfg.tmpDir);

    grunt.initConfig({
        // Configuration
        cfg: cfg,
        userhome: userhome,

        // Watch for file changes and run corresponding tasks
        watch: {
            options: {
                livereload: true,
                interrupt: true,
                livereloadOnError: false
            },
            gruntfile: {
                files: ['Gruntfile.js']
            },
            less: {
                options: {
                    livereload: false
                },
                files: ['src/**/*.less'],
                tasks: ['less', 'autoprefixer', 'csslint', 'concat:css']
            },
            js: {
                files: ['src/**/*.js', '!**/*.spec.js'],
                tasks: ['concat:dist', 'uglify:dist']
            },
            images: {
                files: ['src/images/**/*.{png,jpg,gif}'],
                tasks: ['imagemin']
            },
            livereload: {
                files: [
                    '<%= cfg.demoSrc %>/**/*.html',
                    'dist/css/**/*.css'
                ]
            }
        },

        // clean target (distribution) folder
        clean: {
            bower: [ 'components/**' ],
            dist: [ 'dist/**/*' ]
        },

        // Check if JS files are according to conventions specified in .jshintrc
        jshint: {
            all: [
                'src/**/*.js',
                '!**/*.spec.js'
            ],
            options: {
                'jshintrc': true,
                reporter: require('jshint-stylish')
            }
        },

        // Concat files
        concat: {
            options: {
                stripBanners: true
            },
            dist: {
                src: [
                    'src/shared/*.js',
                    'src/shared/**/*.js',
                    '!src/shared/**/*.spec.js'
                ],
                dest: 'dist/js/main.js'
            },
            css: {
                src: [
                    '.tmp/css/main.css'
                ],
                dest: 'dist/css/main.css'
            }
        },

        // Minify JS files
        uglify: {
            options: {
                preserveComments: 'some'
            },
            dist: {
                files: {
                    'dist/js/main.min.js': ['dist/js/main.js']
                }
            }
        },

        // Lint the css output
        csslint: {
            lessOutput: {
                options: {
                    csslintrc: '.csslintrc'
                },

                src: ['.tmp/css/main.css']
            }
        },

        // Autoprefix vendor prefixes
        autoprefixer: {
            dist: {
                options: {
                    browsers: ['> 0%']
                },
                src: '.tmp/css/main.css',
                dest: '.tmp/css/main.css'
            }
        },

        // Compile LessCSS to CSS.
        less: {
            main: {
                files: {
                    '.tmp/css/main.css': 'src/less/main.less'
                }
            },
            vendors: {
                files: {
                    '.tmp/css/bootstrap.css': 'src/less/bootstrap.less',
                    '.tmp/css/font-awesome.css': 'src/less/font-awesome.less',
                    '.tmp/css/bootstrap-chosen.css': 'src/less/bootstrap-chosen.less'
                }
            }
        },
        // Minify CSS files
        cssmin: {
            options: {
                report: 'min'
            },
            dist: {
                files: {
                    'dist/css/main.min.css': ['dist/css/main.css']
                }
            }
        },

        // Minify images
        imagemin: {
            src: {
                files: [{
                    expand: true,
                    cwd: 'src/images',
                    src: ['**/*.{png,jpg,gif}'],
                    dest: 'src/images/'
                }]
            }
        },

        // Copy files
        copy: {

            dist: {
                files: [
                    {
                        expand: true,
                        cwd: 'components/bootstrap/fonts',
                        src: ['**/*'],
                        dest: 'dist/fonts/'
                    },
                    {
                        expand: true,
                        cwd: 'components/font-awesome/fonts',
                        src: ['**/*'],
                        dest: 'dist/fonts/'
                    },
                    {
                        expand: true,
                        cwd: 'src/images',
                        src: ['**/*.{png,jpg,gif}'],
                        dest: 'dist/images/'
                    }
                ]
            }

        },

        // Testing with karma
        karma: {
            options: {
                configFile: 'karma.conf.js',
                autoWatch: true
            },

            single: {
                singleRun: true,
                browsers: ['PhantomJS']
            },

            continuous: {
                singleRun: false,
                browsers: ['PhantomJS']
            }
        },

        connect: {
            options: {
                port: 9000,
                hostname: '0.0.0.0',
                open: 'http://localhost:9000/#/'
            }
        },

        // Execute commands
        shell: {
            options: {
                stdout: true,
                stderr: true
            }
        }

    });

    // default
    grunt.registerTask('default', [
        'build:dist'
    ]);

    // build dist
    grunt.registerTask('build:dist', 'Build the distribution', [
        'jshint',
        'less',
        'csslint',
        'imagemin',
        'clean:dist',
        'copy:dist',
        'concat',
        'uglify:dist',
        'cssmin:dist'
    ]);

    // test
    grunt.registerTask('test', 'Test the source code', [
        'karma:single'
    ]);

    grunt.registerTask('test:continuous', 'Test the source code continuously', [
        'karma:continuous'
    ]);

};
