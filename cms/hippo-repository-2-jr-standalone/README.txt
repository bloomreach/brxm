PREREQUISITES
-------------
Building the Jackrabbit standalone server requires that the latest Jackrabbit trunk (1.3-SNAPSHOT) is build and installed (locally) first.
Note: use -Dmaven.test.skip=true for building Jackrabbit if you encounter test errors.

BUILDING AND RUNNING
--------------------
mvn compile exec:java

This will start the RMI Server on: rmi://localhost:1099/jr-standalone

To stop the server simply press Ctrl-C in the console.

DEBUGGING
---------
The standalone server can easily be run in debug mode within Eclipse using the m2eclipse plugin: http://m2eclipse.codehaus.org
Flash demos for installing and using the plugin are available from above website.

After installing the plugin, enable it for this project in Eclipse and create an External Tool lauch configuration of type "m2 build" (see flash demo)
and configure it with Base Directory: ${workspace_loc:/hippo-repository-2-jr-standalone} and Goals: exec:java.
