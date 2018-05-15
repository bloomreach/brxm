# Prerequisites
* [Chrome](https://google.chrome.com)
* [Node 8](https://nodejs.org) and [NPM 5](https://www.npmjs.com)
* [gulp-cli](https://github.com/gulpjs/gulp-cli) (optional)

The Hippo Build uses [Gulp.js](https://gulpjs.com) for frontend task automation.
Installing the gulp-cli globally is optional. You can install the gulp-cli
globally via `npm install -g gulp-cli` and use the gulp cli directly, initiate
gulp tasks via `npm run` or simply stick with the predefined `npm start` and
`npm test` described below.

# Development of AngularJS code

1. Add the proxy filter to the web.xml of the cms web app ( for local development only ! )

        <filter>
          <filter-name>ProxyFilter</filter-name>
          <filter-class>org.onehippo.cms7.utilities.servlet.ProxyFilter</filter-class>
          <init-param>
            <param-name>jarPathPrefix</param-name>
            <param-value>/angular</param-value>
          </init-param>
        </filter>

        <filter-mapping>
          <filter-name>ProxyFilter</filter-name>
          <url-pattern>/*</url-pattern>
        </filter-mapping>

2. Build the frontend and start a Webpack dev server.

        npm start

3. Start up your project with Wicket development mode enabled. Make the resource servlet forward all calls for
   localhost:8080/cms/angular/hippo-cm/... to the Webpack dev server.

        cd <your project>
        mvn -Pcargo.run -Dcargo.jvm.args='-Dwicket.configuration=development -Dresource.proxies=angular/hippo-cm@http://localhost:9090/cms'

Mind that if the Webpack dev server has not been started, users see an empty CM perspective without any error message.

# Installation
### Install project dependencies
Run the commands below in the project root directory.

    npm install

# Useful commands
## When starting gulp tasks through npm
Serve and watch files for development

    npm start

Run unit tests and watch for changes to rerun (Chrome needs to be installed)

    npm test

Run unit tests once (Chrome needs to be installed)

    npm run testOnce

List all gulp tasks available

    npm run gulp -- --tasks

If you do not install the gulp-cli you can still run any gulp task with npm

    npm run gulp [some defined gulp task]

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

