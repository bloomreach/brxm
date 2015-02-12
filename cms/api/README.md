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

#####Windows specific instructions
You can automate the installation of NPM using Chocolatey package manager:
https://chocolatey.org/

The package for Node.js can be installed using:

  $ cinst nodejs.install

#### Dependencies

* [Grunt](http://gruntjs.com/) (task automation)
* [Bower](http://bower.io/) (package management)

#### Installation
Run the commands below in the project root directory.
#####1. Install Grunt and Bower

    $ sudo npm install -g grunt-cli bower
    
#####2. Install project dependencies

    $ npm install
    $ bower install

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


