Hippo CMS Theme
===========

The CMS Hippo theme is a new style for Hippo CMS. It utilizes nodejs, grunt and bower to manage the css and js
files that will eventually replace the screen.css beast.

## Development environment setup
#### Prerequisites

* [Node Package Manager](https://npmjs.org/) (NPM)

#### Dependencies

* [Grunt](http://gruntjs.com/) (task automation)
<!--* [Bower](http://bower.io/) (package management)-->

#### Installation
Run the commands below in the project root directory.
#####1. Install Grunt and Bower

<!-- $ sudo npm install -g grunt-cli bower -->
    $ sudo npm install -g grunt-cli
    
#####2. Install project dependencies

    $ npm install
<!-- $ bower install -->

#### Maven
For now we will not call 'grunt build:theme' when building the API module using Maven. Instead, the less files
are compiled using the lesscss-maven-plugin and a minified version of the output is created using the 
minify-maven-plugin. Lastly the fonts are copied using the resources plugin. Although this is not a complete copy
of the 'grunt build:theme' task, it is enough for the time being.

## Useful commands

####Build theme
The theme is located in the `../resources/skin` directory.

    $ grunt build:theme

####Run tests
The tests need to pass in order to build the theme.

    $ grunt test

## Live reload
You need a browser extension to use live reload.

*   Safari: http://download.livereload.com/2.0.9/LiveReload-2.0.9.safariextz
*   Chrome: https://chrome.google.com/webstore/detail/livereload/jnihajbhpnppcggbcgedagnkighmdlei
*   Firefox: http://download.livereload.com/2.0.8/LiveReload-2.0.8.xpi

After it is installed, run

    $ grunt watch
    
It will copy the build resources in 'skin/theme' into the classpath folder, so Wicket will load the new resource.

## Dependencies

####1. Open-Sans 1.1.0
A custom .less file is currently used to define the font-faces. The font files still have to be installed manually.

    $ cd lib
    $ git clone https://github.com/FontFaceKit/open-sans.git
    $ cd open-sans
    $ cp -r fonts/Bold fonts/BoldItalic fonts/Italic fonts/Light fonts/LightItalic fonts/Regular ../../src/fonts/open-sans
    
####2. Normalize.css 3.0.2
Use the less version from bootstrap.

    $ cd lib
    $ git clone https://github.com/twbs/bootstrap.git
    $ cd bootstrap
    $ cp less/normalize.less ../../src/less/lib/


## Future additions

####1. Manage dependencies with Bower
This works out of the box but is postponed due to Maven integration issues.

