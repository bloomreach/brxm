/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

const autoprefixer = require('autoprefixer');
const sass = require('node-sass');
const stylelintFormatter = require('stylelint-formatter-pretty');

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
      sass: {
        options: {
          livereload: false,
        },
        files: ['<%= build.src %>/**/*.scss'],
        tasks: ['stylelint', 'sass', 'postcss']
      },
      reloadCompiledCss: {
        files: ['<%= build.skin %>/**/*.css']
      },
      images: {
        files: ['<%= build.src %>/images/**'],
        tasks: ['newer:copy:binaries']
      },
    },

    // Lint sass files
    stylelint: {
      options: {
        formatter: stylelintFormatter,
      },
      src: [
        '<%= build.src %>/scss/*.scss',
      ]
    },

    // Compile Sass to CSS.
    sass: {
      options: {
        implementation: sass,
        outputStyle: 'expanded',
        sourceMap: true,
      },
      main: {
        files: {
          '<%= build.skin %>/css/<%= build.file %>.css': '<%= build.src %>/scss/main.scss'
        }
      },
    },

    postcss: {
      options: {
        map: {
          inline: false, // save all sourcemaps as separate files
        },

        processors: [autoprefixer()]
      },
      dist: {
        src: '<%= build.skin %>/css/<%= build.file %>.css',
      }
    },

    // Minify CSS files
    cssmin: {
      options: {
        report: 'min',
        sourceMap: true,
        rebaseTo: '<%= build.skin %>/css/'
      },
      theme: {
        files: {
          '<%= build.skin %>/css/<%= build.file %>.min.css': ['<%= build.skin %>/css/<%= build.file %>.css']
        }
      }
    },

    copy: {
      binaries: {
        files: [
          {
            expand: true,
            cwd: '<%= build.npmDir %>/open-sans-fontface/fonts',
            src: ['**/*.{eot,svg,ttf,woff,woff2}'],
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
        ]
      }
    },

    clean: {
      // clean npm components
      npm: {
        src: '<%= build.npmDir %>'
      },

      // clean up copied image, font and css files
      copies: {
        src: ['<%= build.images %>', '<%= build.skin %>']
      }
    },
  });

  grunt.registerTask('default', ['build', 'watch']);

  grunt.registerTask('build', 'Build the login theme', [
    'clean:copies',
    'stylelint',
    'sass',
    'postcss',
    'cssmin:theme',
    'copy:binaries'
  ]);
};
