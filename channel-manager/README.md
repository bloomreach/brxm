# Build
Build with testing:

    $ mvn clean install

Build without testing:

    $ mvn clean install -DskipTests

Build with snapshot dependencies:

    $ mvn clean install -Dhippo.snapshots

    $ mvn clean install "-Dhippo.snapshots" //WINDOWS USERS

 Note: only needed when the project refers to SNAPSHOT dependencies!

 Note: only effective when your Maven settings.xml file contains a profile
       with this property for the Hippo snapshot repository. For details, see
       http://www.onehippo.org/library/development/build-hippo-cms-from-scratch.html

# Develop AngularJS UI
See the frontend-ng/README.md

# Generate Documentation

    $ cd ui-extension
    $ npm run docs
    
The generated documentation is placed in a folder 'docs'.
