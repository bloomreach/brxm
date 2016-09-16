'use strict';

/**
 *  This file contains the variables used in other gulp files
 *  which defines tasks
 *  By design, we only put there very generic config values
 *  which are used in several places to keep good readability
 *  of the tasks
 */

const path = require('path');
const gutil = require('gulp-util');
const pkg = require('../package.json');

exports.ngModule = 'app';

/**
 *  The main paths of your project handle these with care
 */
exports.paths = {
  src: 'src',
  dist: 'target/classes/angular/hippo-cm',
  tmp: 'target/classes/angular/hippo-cm',
  e2e: 'e2e',
  tasks: 'gulp_tasks',
  npmDir: 'node_modules',
};

exports.exclude = {
  vendors: [
    'open-sans-fontface',
  ],
};

exports.vendors = Object.keys(pkg.dependencies)
  .filter(name => exports.exclude.vendors.indexOf(name) === -1);

exports.path = {};
for (const pathName in exports.paths) {
  if (exports.paths.hasOwnProperty(pathName)) {
    exports.path[pathName] = function pathJoin() {
      const pathValue = exports.paths[pathName];
      const funcArgs = Array.prototype.slice.call(arguments);
      const joinArgs = [pathValue].concat(funcArgs);
      return path.join.apply(this, joinArgs);
    };
  }
}

/**
 *  Common implementation for an error handler of a Gulp plugin
 */
exports.errorHandler = function (title) {
  return function (err) {
    gutil.log(gutil.colors.red(`[${title}]`), err.toString());
    this.emit('end');
  };
};
