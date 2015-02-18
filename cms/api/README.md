Hippo CMS Theme
===========

The CMS Hippo theme is a new style for Hippo CMS. It utilizes nodejs, grunt and bower to manage the css and js
files that will eventually replace the screen.css beast.

Follow the instructions below to install the required dependencies. Please note that the Maven build will also run the
commands found in step 3 when executing `mvn install` on the API module.

## Development environment setup
#### Prerequisites

* [Java 8] (https://jdk8.java.net)
* [Maven 3.x] (http://maven.apache.org)
* [Git] (http://git-scm.com)
* [NodeJS 0.10.x] (https://nodejs.org/)
* [NPM] (https://npmjs.org/) (Node package manager)
* [Grunt 0.4.5] (http://gruntjs.com/) (Task automation)
* [Bower 1.2.6] (http://bower.io/) (Package management for the web)

#####Windows specific preparation instructions
You can automate the installation of NodeJS and NPM using [Chocolatey package manager]
(https://chocolatey.org).

The package for Node.js can be installed using (this will also install NPM):

    C:\> choco install nodejs.install

__Note:__ Due to a [bug] (http://jira.codehaus.org/browse/MEXEC-137) in the exec-maven-plugin the Maven build of the
API module will fail as the exec-maven-plugin is unable to find the __grunt__ and __bower__ commands. To fix this do
the following:

* open folder `C:\Users\USER\AppData\Roaming\npm`
* copy __grunt.cmd__ to __grunt.bat__
* copy __bower.cmd__ to __bower.bat__

More information can be found [here] (http://stackoverflow.com/questions/22393785/exec-maven-plugin-says-cannot-run-specified-program-even-though-it-is-on-the-pa/22395703#22395703).

Installation of GIT is also possible using Chocolatey:

    C:\> choco install git.install

#### Installation
Run the commands below in the __api__ directory.

#####1. Install Grunt and Bower
Before setting up Grunt ensure that your npm is up-to-date by running

    npm update -g npm

(this might require `sudo` on certain systems).

#####2. Install Grunt and Bower

    npm install -g grunt-cli bower

(this might require `sudo` on certain systems).

#####3. Install project dependencies
Install both npm and bower dependencies

    npm install
    bower install

## Useful commands

####Build theme

    grunt build

####Live reload
You need a browser extension to use live reload.

*   Safari: [http://download.livereload.com/2.0.9/LiveReload-2.0.9.safariextz] (http://download.livereload.com/2.0.9/LiveReload-2.0.9.safariextz)
*   Chrome: [https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei] (https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei)
*   Firefox: [http://download.livereload.com/2.0.8/LiveReload-2.0.8.xpi] (http://download.livereload.com/2.0.8/LiveReload-2.0.8.xpi)

After it is installed, run

    grunt watch

This will handle the following cases:

* If a change in a LESS file is detected it will recompile all LESS files, copy the theme to the CLASSPATH and (if
enabled) live-reload your browser.
* If a change in an image is detected it will re-create the SVG sprite and copy all images and icons to the CLASSPATH.


####Starting Wicket in development mode so it picks up Grunt changes

Run the CMS with Wicket development mode args:

    mvn -P cargo.run -Djrebel -Drepo.path=repo -Dcargo.jvm.args='-Dwicket.configuration=development'
