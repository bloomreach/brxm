BUILDING AND RUNNING
--------------------
mvn clean compile exec:java

This will start the RMI Server on: rmi://localhost:1099/jackrabbit.repository

To stop the server simply press Ctrl-C in the console.

VIEWING
-------

In order to use the supplied facet-compliant plain JCR viewer you
should have a plain Tomcat 5.5 installation at hand.  copy the file
"jcrviewer.jsp" into the webapps/ROOT directory of the Tomcat 5.5
installation.  Also copy the JARs:
  jackrabbit-jcr-rmi-1.3-r529494.jar
  jcr-1.0.jar
from your maven 2 repository into the "common/lib/" directory of your
Tomcat installation.
Restart tomcat and access the JCR viewer URL (most of the cases this will
be http://localhost:8080/jcrviewer.jsp).

If you run the JCR repository on a machine different from your Tomcat, then
you must modify the jcrviewer.jsp to indicate the right machine to connect to.
This is specified on the line containing "repositoryFactory.getRepository".

DEBUGGING
---------
The standalone server can easily be run in debug mode within Eclipse
using the m2eclipse plugin: http://m2eclipse.codehaus.org
Flash demos for installing and using the plugin are available from above website.

After installing the plugin, enable it for this project in Eclipse
and create an External Tool lauch configuration of type "m2 build"
(see flash demo) and configure it with Base Directory:
${workspace_loc:/hippo-repository-2-jr-standalone}
and Goals: exec:java.

NOTES
-----
You MUST have your maven 2 repository on a path which does not have any
spaces in them.  This applies especially for Windows users.
