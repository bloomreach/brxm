Running locally
===============

This project uses the Maven Cargo plugin to run Essentials, the CMS and site locally in Tomcat.
From the project root folder, execute:

  mvn clean verify
  mvn -P cargo.run

By default this includes and bootstraps repository content from the bootstrap/content module,
which is deployed by cargo to the Tomcat shared/lib.
If you want or need to start *without* bootstrapping the local content module, for example when testing
against an existing repository, you can specify the *additional* Maven profile without-content to do so:

  mvn -P cargo.run,without-content

This additional profile will modify the target location for the content module to the Tomcat temp/ folder so that
it won't be seen and picked up during the repository bootstrap process.

Access the Hippo Essentials at http://localhost:8080/essentials.
After your project is set up, access the CMS at http://localhost:8080/cms and the site at http://localhost:8080/site.
Logs are located in target/tomcat8x/logs

Building distributions
======================

To build Tomcat distribution tarballs:

  mvn clean verify
  mvn -P dist
    or
  mvn -P dist-with-content

The 'dist' profile will produce in the /target directory a distribution tarball, containing the main deployable wars and
shared libraries.

The 'dist-with-content' profile will produce a distribution-with-content tarball, containing as well the
bootstrap-content jar in the shared/lib directory. This kind of distribution is meant to be used for deployments on
empty repositories, for instance deployment on a new environment.

See also src/main/assembly/*.xml if you need to customize the distributions.

Using JRebel
============

Set the environment variable REBEL_HOME to the directory containing jrebel.jar.

Build with:

  mvn clean verify -Djrebel

Start with:

  mvn -P cargo.run -Djrebel

Best Practice for development
=============================

Use the option -Drepo.path=/some/path/to/repository during start up. This will avoid
your repository to be cleared when you do a mvn clean.

For example start your project with:

  mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo

or with jrebel:

  mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo -Djrebel

Hot deploy
==========

To hot deploy, redeploy or undeploy the CMS or site:

  cd cms (or site)
  mvn cargo:redeploy (or cargo:undeploy, or cargo:deploy)

Automatic Export
================

Automatic export of repository changes to the filesystem is turned on by default. To control this behavior, log into
http://localhost:8080/cms/console and press the "Enable/Disable Auto Export" button at the top right. To set this
as the default for your project edit the file
./bootstrap/configuration/src/main/resources/configuration/modules/autoexport-module.xml

Monitoring with JMX Console
===========================
You may run the following command:

  jconsole

Now open the local process org.apache.catalina.startup.Bootstrap start
