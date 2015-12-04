var hippoBuild = require('hippo-build');
var customConfig = require('./build.conf.js');
var gulp = require('gulp');

hippoBuild.buildTasks(customConfig);

function copyDistToTarget() {
  var cfg = hippoBuild.buildConfig(customConfig);
  return gulp.src(cfg.distDir + '/**')
    .pipe(gulp.dest('target/classes/angular/cmng'));
}

function buildMvn(done) {
  gulp.series('buildDist', copyDistToTarget)(done);
}

gulp.task(buildMvn);
