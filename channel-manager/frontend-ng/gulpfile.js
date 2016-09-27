const del = require('del');
const gulp = require('gulp');
const HubRegistry = require('gulp-hub');
const conf = require('./conf/gulp.conf');

// Load tasks into the registry
const hub = new HubRegistry([conf.path.tasks('*.js')]);

// Tell gulp to use the tasks just loaded
gulp.registry(hub);

function clean() {
  return del([conf.paths.dist, conf.paths.tmp]);
}

gulp.task('test', gulp.series('karma:single-run'));
gulp.task('test:auto', gulp.series('karma:auto-run'));
gulp.task('serve', gulp.series('webpack:serve'));
gulp.task('serveDist', gulp.series('webpack:distServe'));
gulp.task('clean', clean);
gulp.task('build', gulp.series('clean', 'webpack:dist'));
gulp.task('default', gulp.series('build'));
