Hippo CMS Theme
===========

The CMS Hippo theme is a new style for Hippo CMS. It utilizes nodejs, grunt and bower to manage the css and js
files that will eventually replace the screen.css beast.

## Development environment setup
#### Prerequisites

* Java 7
* Maven 3.x
* [NodeJS](https://nodejs.org/) (NodeJS)
* [Node Package Manager](https://npmjs.org/) (NPM)
* [NPM-cache 0.1.0] (https://github.com/swarajban/npm-cache) (NPM and Bower dependencies cache)

#####Windows specific instructions
You can automate the installation of NPM using Chocolatey package manager:
https://chocolatey.org/

The package for Node.js can be installed using:

  $ cinst nodejs.install

#### Installation
Run the commands below in the project root directory.
#####1. Update npm
Before setting up Grunt ensure that your npm is up-to-date by running

    npm update -g npm

(this might require `sudo` on certain systems).
#####2. Install Grunt, Bower and npm-cache

    npm install -g grunt-cli bower npm-cache

(this might require `sudo` on certain systems).

#####3. Install project dependencies
Install both npm and bower dependencies

    npm-cache install

While the above is the easiest and also used by the Maven build process, we have a few more options.

Install npm dependencies

    npm-cache install npm

Install bower dependencies

    npm-cache install bower

Clean the cached dependencies

    npm-cache clean

Although it is pretty save to cache dependencies (as we try to use specific versions instead of ranges), you might want
to ensure yourself that cache is not the issue.

Install fresh npm dependencies (requires network)

    npm install

Install fresh dependencies (requires network)

    bower install

## Useful commands

####Build theme
The theme is located in the `../resources/skin/hippo-cms` directory.

    $ grunt build

####Install theme
The theme is build and copied to the classpath of the api module

    $ grunt install

####Live reload
You need a browser extension to use live reload.

*   Safari: http://download.livereload.com/2.0.9/LiveReload-2.0.9.safariextz
*   Chrome: https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei
*   Firefox: http://download.livereload.com/2.0.8/LiveReload-2.0.8.xpi

After it is installed, run

    $ grunt watch
    
It will copy the build resources in 'skin/hippo-cms' into the classpath folder, so Wicket will load the new resource.

####Starting Wicket in development mode so it picks up Grunt changes

Run the CMS with Wicket development mode args:

    $ mvn -P cargo.run -Djrebel -Drepo.path=repo -Dcargo.jvm.args='-Dwicket.configuration=development'


####Run tests
The tests need to pass in order to build the theme.

    $ grunt test

