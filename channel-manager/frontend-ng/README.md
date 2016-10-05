# Prerequisites
* [Chrome](http://google.chrome.com)
* [gulp-cli](https://github.com/gulpjs/gulp-cli) (optional)

The Hippo Build uses [Gulp.js](https://gulpjs.com) for frontend task automation.
Installing the gulp-cli globally is optional. You can install the gulp-cli
globally via `npm install -g gulp-cli` and use the gulp cli directly, initiate
gulp tasks via `npm run` or simply stick with the predefined `npm start` and
`npm test` described below.

# Installation
### Install project dependencies
Run the commands below in the project root directory.

    $ npm install

# Development of AngularJS code

1. Compile channel manager with JRebel

        $ mvn clean install -Djrebel

2. Start up your project with JRebel and wicket development mode enabled

        $ cd <your project>
        $ mvn -Pcargo.run -Dcargo.jvm.args='-Dwicket.configuration=development' -Djrebel

3. Start up frontend build system

        $ npm start

# Useful commands
## When starting gulp tasks using cli
### List all gulp tasks available

    $ gulp --tasks

### Run unit tests (Chrome needs to be installed)

    $ gulp test

### Build application for development

    $ gulp build

### Build optimized application for production

    $ gulp buildDist

### Serve and watch files for development with BrowserSync

    $ gulp serve

## When starting gulp tasks through npm
If you do not install the gulp-cli you can run the following with npm

    $ npm run gulp [some defined gulp task]

### List all gulp tasks available

    $ npm run gulp -- --tasks

### Run unit tests (Chrome needs to be installed)
    $ npm test

### Serve and watch files for development with BrowserSync
    $ npm start

