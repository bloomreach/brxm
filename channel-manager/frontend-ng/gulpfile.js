const gulp = require('gulp');
const HubRegistry = require('gulp-hub');
const conf = require('./conf/gulp.conf');

// Load tasks into the registry
const hub = new HubRegistry([conf.path.tasks('*.js')]);

// Tell gulp to use the tasks just loaded
gulp.registry(hub);

gulp.task('test', gulp.series('karma:single-run'));
gulp.task('test:auto', gulp.series('karma:auto-run'));
gulp.task('build', gulp.series('clean', 'other', 'webpack:dist'));
gulp.task('serve', gulp.series('clean', 'other', 'webpack'));
gulp.task('default', gulp.series('build'));
