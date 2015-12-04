# Hippo Angular Channel Manager Next Generation
Just change the name in the package.json, bower.json and src/* files to the name of your application.

## Development environment setup
### Prerequisites
* [NodeJS](http://nodejs.org/) (NodeJS)
* [Node Package Manager](https://npmjs.org/) (NPM, comes installed with NodeJS)
* [Git](http://git-scm.com/)

### Dependencies
* [Bower](http://bower.io/) (front-end package manager)
* [Gulp](http://gulpjs.com/) (task automation)

### Gulp4 / Gulp3
The Hippo Build gulp uses gulp4, which is currently not released yet. You can install gulp4 globally via
the command listed below. If you need to have gulp3 installed globally we have set up the npm script 'gulp' so
you can run the build tasks with the following command:
  $ npm run gulp [some task]

### Installation
#### Install Gulp CLI and Bower globally
  $ npm install -g gulpjs/gulp-cli#4.0
  $ npm install -g bower

#### Install project dependencies
Run the commands below in the project root directory.

  $ npm install
  $ bower install

## Useful commands
### List all gulp tasks available
  $ gulp --tasks

### Run unit tests
  $ gulp unitTests

### Build application for development
  $ gulp build

### Setup development server
  $ gulp server

### Build optimized application for production
  $ gulp buildDist

### Setup production server
  $ gulp serverDist
