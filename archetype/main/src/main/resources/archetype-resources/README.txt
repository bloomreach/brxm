#set($dollar = '$')
#set($symbol_pound = '#')
#set($symbol_dollar = '$')
#set($symbol_escape = '\')
#set($hyphen = '-')
#set($empty = '')
Running Locally
===============

This project uses the Maven Cargo plugin to run Essentials, the CMS and site locally in Tomcat.
From the project root folder, execute:

    mvn clean verify
    mvn -P cargo.run

By default this includes and bootstraps repository data from the repository-data/development module,
which is deployed by cargo to the Tomcat shared/lib.
If you want or need to start *without* bootstrapping the development data, for example when testing
against an existing repository, you can specify the *additional* Maven profile without-development-data to do so:

    mvn -P cargo.run,without-development-data

This additional profile will modify the target location for the development module to the Tomcat temp/ folder so that
it won't be seen and picked up during the repository bootstrap process.

Access the BloomReach setup application at <http://localhost:8080/essentials>.
After your project is set up, access the CMS at <http://localhost:8080/cms> and the site at <http://localhost:8080/${rootArtifactId.replace($hyphen,$empty)}>.
Logs are located in target/tomcat9x/logs


Best Practice for Development
=============================

Use the option `-Drepo.path=/some/path/to/repository` during start up. This will avoid
your repository to be cleared when you do a mvn clean.

For example start your project with:

    mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo


Automatic Export
================

Automatic export of repository changes to the filesystem is turned on by default. To control this behavior, log into
<http://localhost:8080/cms/console> and press the "Enable/Disable Auto Export" button at the top right. To set this
as the default for your project edit the file
./repository-data/application/src/main/resources/hcm-config/configuration/modules/autoexport-module.yaml


Building Distributions
======================

To build Tomcat distribution tarballs:

    mvn clean verify
    mvn -P dist
      or
    mvn -P dist-with-development-data

The `dist` profile will produce in the /target directory a distribution tarball, containing the main deployable wars and
shared libraries.

The `dist-with-development-data` profile will produce a distribution-with-development-data tarball, also containing the
repository-data-development jar in the shared/lib directory. This kind of distribution is meant to be used for
deployments to development environments, for instance local deployments or deployments to a continuous integration (CI)
system. (Initially, this module contains only "author" and "editor" example users for use in testing. Other data must be
placed in this module explicitly by developers, for demo or testing purposes, etc.)

See also src/main/assembly/*.xml if you need to customize the distributions.


Distributing Additional Site Projects
=====================================

Note that if your organization is using multiple site projects, you must configure the assembly of a distribution to
include all of the separate site webapps for deployment. This project is designed for stand-alone use and does not
automatically include any additional, externally-maintained site webapps.
