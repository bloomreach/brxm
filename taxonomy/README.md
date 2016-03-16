# Taxonomy Plugin

This is the Taxonomy plugin for Hippo CMS. Documentation on how to use this
plugin in your Hippo project can be found at:

http://www.onehippo.org/library/concepts/plugins/taxonomy/about.html

## Plugin Demo

A demo project is provided to see the plugin in action.

### Requirements

* Java 8 JDK
* Maven 3

### Download, Build and Run the Demo Project

1. Download the plugin's source code from https://code.onehippo.org/cms-community/hippo-plugin-taxonomy
    * Alternatively you can clone the git repository on your local machine.
    * Use the latest stable branch named "release/x" (or "demo/x" in case the
    demo is tagged seperately) where "x" is the version number.
    * If you want to build the master branch demo you must configure the Hippo
    Maven snapshot repository. See
    http://www.onehippo.org/library/development/build-hippo-cms-from-scratch.html
    for instructions.
2. Extract the archive on your local file system.
3.  Use Maven to build and run the demo project:  
    ```bash
    cd hippo-plugin-taxonomy/demo
    mvn verify
    mvn -P cargo.run
    ```

4.  Hippo CMS will be available at http://localhost:8080/cms/ (login using
    credentials `admin`/`admin`).  
    The demo website will be available at http://localhost:8080/site/.
    
### Development Branches

To build and run the demo from a development branch you may need to configure
access to the Hippo Maven snapshots repository. For more information see the
following documentation page:

http://www.onehippo.org/library/development/build-hippo-cms-from-scratch.html

