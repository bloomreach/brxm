# Developer documentation

This document summarizes information relevant to BloomReach Open UI SDK developers.

## Prerequisites

* Maven 3
* Node 8 (optional)
* NPM 6 (optional)

Maven build installs a local copy of Node 8 and NPM 6 automatically.
Frontend developers can also use their global installation instead. 

## Build

Build with Maven:

    $ mvn clean install 
    
## Develop

Build manually (and watch for changes):

    $ npm run build -- --watch
    
Run tests (and watch for changes):

    $ npm run test -- --watch

Run linter:

    $ npm run lint
    
## Generate documentation

Generate Typedoc files:

    $ npm run docs
    
The generated documentation is placed in a folder 'docs'.

## Release NPM package

The Maven build only generates a JAR file. The main audience of this library are NPM users, though.
The NPM package must be released with the Bash script, after the build has run:

     $ mvn clean verify
     $ ./release-npm-package.sh <semantic-version-to-release>
     
The script releases the UI extension package on NPM central.

Requirements:
- [NPM user account](https://www.npmjs.com/login)
- NPM user should be part of the [@bloomreach organization](https://www.npmjs.com/settings/bloomreach/members)

### Un-releasing

A released package can be un-released with 

    $ npm unpublish @bloomreach/ui-extension@[version]

Un-releasing can only be done within 72 hours after a release.

RELEASED VERSIONS CAN NEVER BE USED AGAIN, so releasing again needs to increase the patch version.
