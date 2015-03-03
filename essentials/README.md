# Hippo Essentials
Hippo Essentials is for developers who want to setup a new [Hippo CMS](http://www.onehippo.org) project. It enables
them to kickstart their project in a matter of minutes, to benefit from our best practices and to easily add Enterprise
or community plugins from the Hippo Marketplace.

```
Please use the Hippo Essentials feedback form to inform us if you encounter any bugs/glitches or if you have any
suggestions for improvements.
```

# Getting Started

## SVN checkout

To get started with the Hippo Essentials checkout the trunk. You have two options to check out
the project.

### Read-only
```shell
svn co  http://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk  essentials
```

### Read-write (you'll need Hippo SVN account for this)
```shell
svn co  https://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk  essentials
```

### Build the essentials components:
```shell
cd essentials
mvn clean install
```

### Validate license headers:
```shell
mvn clean && mvn validate -Ppedantic
```

### Create and install archetype locally:
```shell
svn co  http://svn.onehippo.org/repos/hippo/hippo-cms7/archetype/trunk/ archetype
cd archetype
mvn clean install
```

### run archetype
```shell
mvn archetype:generate -D "archetypeGroupId=org.onehippo.cms7" -D "archetypeArtifactId=hippo-project-archetype" -D "archetypeVersion=2.01.00-SNAPSHOT"
```

##Running locally


This project uses the Maven Cargo plugin to run the CMS, Website and Essentials dashboard locally in Tomcat.
From the project root folder, execute:

```shell
mvn clean install
mvn -P cargo.run
```

The following URLs are available from this project:

 * CMS at http://localhost:8080/cms
 * Website at http://localhost:8080/site 
 * Essentials dashboard at http://localhost:8080/essentials

Logs are located in `target/tomcat8x/logs`

##Using JRebel

Set the environment variable `REBEL_HOME` to the directory containing jrebel.jar.

Build with:

```shell
mvn clean install -Djrebel
```

Start with:

```shell
mvn -P cargo.run -Djrebel
```

##Best Practice for development

Use the option `-Drepo.path=/some/path/to/repository` during start up. This will avoid
your repository to be cleared when you do a mvn clean.

For example start your project with:

```shell
mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo
```
or with jrebel:

```shell
mvn -P cargo.run -Drepo.path=/home/usr/tmp/repo -Djrebel
```
##Hot deploy

To hot deploy, redeploy or undeploy the CMS or site:

```shell
cd cms (or site)
mvn cargo:redeploy (or cargo:undeploy, or cargo:deploy)
```

##Automatic Export

To have your repository changes automatically exported to filesystem during local development, log into
http://localhost:8080/cms/console and press the *"Enable Auto Export"* button at the top right. To set this
as the default for your project edit the file
`./bootstrap/configuration/src/main/resources/configuration/modules/autoexport-module.xml`

##Copyright and license

Copyright 2013-2015 Hippo B.V.
Distributed under the [Apache 2.0 license](http://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk/LICENSE).

