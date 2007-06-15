BUILDING AND RUNNING
--------------------
mvn clean jetty:run

Make sure you have an RMI Server running on: rmi://localhost:1099/jackrabbit.repository

and browse to
http://localhost:8080/hippo-repository-2-webapp

To stop the server simply press Ctrl-C in the console.

NOTES
-----
You MUST have your maven 2 repository on a path which does not have any
spaces in them.  This applies especially for Windows users.
