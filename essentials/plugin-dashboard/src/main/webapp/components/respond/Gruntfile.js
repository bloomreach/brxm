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

module.exports = function(grunt) {
	"use strict";

	// Project configuration.
	grunt.initConfig({
		pkg: grunt.file.readJSON('package.json'),
		banner:
						'/*! Respond.js v<%= pkg.version %>: <%= pkg.description %>' +
						' * Copyright <%= grunt.template.today("yyyy") %> <%= pkg.author.name %>\n' +
						' * Licensed under <%= _.pluck(pkg.licenses, "url").join(", ") %>\n' +
						' * <%= pkg.website %>' +
						' */\n\n',
		uglify: {
			nonMinMatchMedia: {
				options: {
					mangle: false,
					compress: false,
					preserveComments: 'some',
					beautify: {
						beautify: true,
						indent_level: 2
					}
				},
				files: {
					'dest/respond.src.js': ['src/matchmedia.polyfill.js', 'src/respond.js']
				}
			},
			minMatchMedia: {
				options: {
					banner: '<%= banner %>'
				},
				files: {
					'dest/respond.min.js': ['src/matchmedia.polyfill.js', 'src/respond.js']
				}
			},
			nonMinMatchMediaListener: {
				options: {
					mangle: false,
					compress: false,
					preserveComments: 'some',
					beautify: {
						beautify: true,
						indent_level: 2
					}
				},
				files: {
					'dest/respond.matchmedia.addListener.src.js': ['src/matchmedia.polyfill.js', 'src/matchmedia.addListener.js', 'src/respond.js']
				}
			},
			minMatchMediaListener: {
				options: {
					banner: '<%= banner %>'
				},
				files: {
					'dest/respond.matchmedia.addListener.min.js': ['src/matchmedia.polyfill.js', 'src/matchmedia.addListener.js', 'src/respond.js']
				}
			}
		},
		jshint: {
			files: ['src/respond.js', 'src/matchmedia.polyfill.js'],
			options: {
				curly: true,
				eqeqeq: true,
				immed: true,
				latedef: false,
				newcap: true,
				noarg: true,
				sub: true,
				undef: true,
				boss: true,
				eqnull: true,
				smarttabs: true,
				node: true,
				es5: true,
				strict: false
			},
			globals: {
				Image: true,
				window: true
			}
		}
	});

	grunt.loadNpmTasks( 'grunt-contrib-jshint' );
	grunt.loadNpmTasks( 'grunt-contrib-uglify' );

	// Default task.
	grunt.registerTask('default', ['jshint', 'uglify']);

};
