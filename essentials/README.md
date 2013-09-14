#Hippo Essentials
===

Hippo Essentials is a project aimed at speeding up [Hippo CMS](http://www.onehippo.org) project implementations. It allows developers to easily install and configure Hippo CMS functionalities.

# Getting Started

To get started with the Hippo Essentials checkout the current master branch.

```
$ git clone https://github.com/onehippo/essentials.git
```

##Running locally


This project uses the Maven Cargo plugin to run the CMS, Website and Essentials dashboard locally in Tomcat.
From the project root folder, execute:

```
$ mvn clean install
$ mvn -P cargo.run
```

The following URLs are available from this project:

 * CMS at http://localhost:8080/cms
 * Website at http://localhost:8080/site 
 * Essentials dashboard at http://localhost:8080/dashboard

Logs are located in `target/tomcat6x/logs`

##Using JRebel


Set the environment variable `REBEL_HOME` to the directory containing jrebel.jar.

Build with:

```
$ mvn clean install -Djrebel
```

Start with:

```
$ mvn -P cargo.run -Djrebel
```

##Best Practice for development


Use the option `-Drepo.path=/some/path/to/repository` during start up. This will avoid
your repository to be cleared when you do a mvn clean.

For example start your project with:

```
$ mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo
```
or with jrebel:

```
$ mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo -Djrebel
```
##Hot deploy

To hot deploy, redeploy or undeploy the CMS or site:

```
$ cd cms (or site)
$ mvn cargo:redeploy (or cargo:undeploy, or cargo:deploy)
```

##Automatic Export


To have your repository changes automatically exported to filesystem during local development, log into
http://localhost:8080/cms/console and press the *"Enable Auto Export"* button at the top right. To set this
as the default for your project edit the file
`./bootstrap/configuration/src/main/resources/configuration/modules/autoexport-module.xml`

##Copyright and license

Copyright 2013 Hippo B.V under the [Apache 2.0 license](https://github.com/onehippo/essentials/blob/master/LICENSE).