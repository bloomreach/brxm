Running locally
===============

Hippo GoGreen uses the Maven Cargo plugin to run the CMS and site locally in Tomcat.
From the project root folder, execute:

  $ mvn clean install -DskipTests
  $ mvn -P cargo.run

Access the CMS at http://localhost:8080/cms, and the site at http://localhost:8080/site
Logs are located in target/tomcat6x/logs

Using JRebel
============

Set the environment variable REBEL_HOME to the directory containing jrebel.jar.

Build with:

  $ mvn -Djrebel

or add -Djrebel to your MAVEN_OPTS environment variable:

  $ export MAVEN_OPTS="$MAVEN_OPTS -Djrebel"

Note: the latter *always* enables JRebel. To disable temporarily (e.g. when building/deploying a release) use

  $ mvn -P -jrebel

Do *not* activate JRebel using "mvn -P jrebel", as it then deactivates the "default" profile.

Hot deploy
==========

To hot deploy, redeploy or undeploy the CMS or site:

  $ cd cms (or site)
  $ mvn cargo:redeploy (or cargo:undeploy, or cargo:deploy)
