Running locally
===============

This project uses the Maven Cargo plugin to run the CMS and site locally in Tomcat.
From the project root folder, execute:

  $ mvn clean install
  $ mvn -P cargo.run

Access the CMS at http://localhost:8080/cms, and the site at http://localhost:8080/site
Logs are located in target/tomcat6x/logs

Building distribution
=====================

To build a Tomcat distribution tarball containing only deployable artifacts:

  $ mvn clean install
  $ mvn -P dist

See also src/main/assembly/distribution.xml if you need to customize the distribution

Using JRebel
============

Set the environment variable REBEL_HOME to the directory containing jrebel.jar.

Build with:

  $ mvn clean install -Djrebel

Start with:

  $ mvn -P cargo.run -Djrebel

Available webapps/hosts/sites
============
http://localhost:8080/cms
http://localhost:8080/cms/console
http://localhost:8080/cms/repository
http://localhost:8080/site  (corporate site)
http://localhost:8080/site/demosite_fr  (french sub-site)

http://com.localhost:8080/site (environment to add new sites)
http://dev.example.com:8080/intranet (intranet is different webapp!!!)

Note for the last two URLs you need to make sure the hosts are added to /etc/hosts (ubuntu) and have
IP address 127.0.0.1 associated.

Best Practice for development
============

Use the option -Drepo.path=/some/path/to/repository during start up. This will avoid
your repository to be cleared when you do a mvn clean. 

For example start your project with: 

$ mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo

or with jrebel:

$ mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo -Djrebel

Hot deploy
==========

To hot deploy, redeploy or undeploy the CMS or site:

  $ cd cms (or site)
  $ mvn cargo:redeploy (or cargo:undeploy, or cargo:deploy)
  
Automatic Export
==========

To have your repository changes automatically exported to filesystem during local development, add to the 
startup cmd: 

-Dhippoecm.export.dir=content/src/main/resources 

The automatic export can then be switch off/on through the /cms/console. By default it is switched on.
  
Monitoring with JMX Console
===========================
You may run the following command:

  $ jconsole service:jmx:rmi:///jndi/rmi://localhost:9875/jmxrmi
  
  
Adding a debugger like Yourkit
=============================
-Dcargo.jvm.args="-Xmx200m -Xms200m -agentpath:/usr/local/yourkit/yjp-9.0.9/bin/linux-x86-64/libyjpagent.so"


Clustered Setup Against MySQL
===============================================

The testsuite can be used to very easily run a clustered setup up where the repository and targeting data
are stored in MySQL

To run the testsuite clustered against MySQL, do the following:

Make sure you have MySQL accessible. Default configuration is:

  <mysql.username>root</mysql.username>
  <mysql.password></mysql.password>
  <mysql.host>localhost:3306</mysql.host>
  <mysql.repo.db>testsuite</mysql.repo.db>

Make sure the MySQL server contains the database 'testsuite'. If you want different settings,
change them in /pom.xml 'mysql' profile

After building the testsuite, copy the 'testsuite' folder to 'testsuite-node2' folder (different location)

Then startup as follows:

~/myprojects/testsuite $ mvn -P cargo.run,mysql
~/myprojects/testsuite-node2 $ mvn -P cargo.run,mysql,node2

After this, the website is available at localhost:8080/site AND at localhost:9080/site. CMSes are
available at localhost:8080/cms and localhost:9080/cms. Note that the cms at localhost:9080/cms does not display
channels in the channel mngr. This is because this setup with cargo
is of course not a real production supported setup. It serves to validate repository clustering.

The clustering of course does show changes made via localhost:9080/cms on the site on port 8080.

Last thing: Please realize that an http session is regardless port number. So localhost:8080 and localhost:9080 share
the same cookies. Imho, a browser flaw. Best to use different browser to circumvent this