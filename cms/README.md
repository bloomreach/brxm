# Build
Build with testing:

    $ mvn clean install

Build without testing:

    $ mvn clean install -DskipTests

Build with snapshot dependencies:

    $ mvn clean install -Dhippo.snapshots=true

 Note: only needed when the project refers to SNAPSHOT dependencies!

 Note: only effective when your Maven settings.xml file contains a profile
       with this property for the Hippo snapshot repository. For details, see
       http://www.onehippo.org/library/development/build-hippo-cms-from-scratch.html

# Front-end Installation
* Run these commands from either the api or the console/frontend module

Install project dependencies

    $ npm install

# Building and developing the front-end theme
Build front-end code:

    $ npm run build

After building the frontend-end code:

    $ npm start

This will handle the following cases:
    * If a change in a LESS file is detected it will recompile all LESS files, copy the theme to the CLASSPATH and (if
      enabled) live-reload your browser.
    * If a change in an image is detected it will re-create the SVG sprite and copy all images and icons to the CLASSPATH.

You need to start Wicket in development mode so it picks up Grunt changes
Run the CMS with Wicket development mode args:

    $ mvn -P cargo.run -Djrebel -Drepo.path=repo -Dcargo.jvm.args='-Dwicket.configuration=development'

# Live reload browser plugins
You need a browser extension to use live reload.

Safari:
http://download.livereload.com/2.1.0/LiveReload-2.1.0.safariextz

Chrome:
https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei

Firefox:
http://download.livereload.com/2.1.0/LiveReload-2.1.0.xpi
