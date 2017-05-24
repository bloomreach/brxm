# Prerequisites
* [Chrome](http://google.chrome.com)
* [yarn](https://yarnpkg.com)
* [gulp-cli](https://github.com/gulpjs/gulp-cli) (optional)

The Hippo Build uses [Gulp.js](https://gulpjs.com) for frontend task automation.
Installing the gulp-cli globally is optional. You can install the gulp-cli
globally via `yarn global add gulp-cli` and use the gulp cli directly, initiate
gulp tasks via `yarn run` or simply stick with the predefined `yarn start` and
`yarn test` described below.

# Development of AngularJS code

1. Build the frontend and start a Webpack dev server.

        yarn start

2. Start up your project with Wicket development mode enabled. Make the resource servlet forward all calls for
   localhost:8080/cms/angular/hippo-cm/... to the Webpack dev server.

        cd <your project>
        mvn -Pcargo.run -Dcargo.jvm.args='-Dwicket.configuration=development -Dresource.proxies=angular/hippo-cm@http://localhost:9090/cms'

# Installation
### Install project dependencies
Run the commands below in the project root directory.

    yarn

# Useful commands
## When starting gulp tasks through npm
Serve and watch files for development

    yarn start

Run unit tests and watch for changes to rerun (Chrome needs to be installed)

    yarn test

Run unit tests once (Chrome needs to be installed)

    yarn run testOnce

List all gulp tasks available

    yarn run gulp -- --tasks

If you do not install the gulp-cli you can still run any gulp task with npm

    yarn run gulp [some defined gulp task]

## When starting gulp tasks using gulp-cli
List all gulp tasks available

    gulp --tasks

Serve and watch files for development

    gulp start

Run unit tests and watch for changes to rerun (Chrome needs to be installed)

    gulp test

Run unit tests once (Chrome needs to be installed)

    gulp testOnce

Build optimized application for production

    gulp build

