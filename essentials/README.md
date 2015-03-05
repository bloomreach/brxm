# Hippo Essentials
Hippo Essentials is for developers who want to setup a new [Hippo CMS](http://www.onehippo.org) project. It enables
them to kickstart their project in a matter of minutes, to benefit from our best practices and to easily add Enterprise
or community plugins from the Hippo Marketplace.

```
Please use the Hippo Essentials feedback form to inform us if you encounter any bugs/glitches or if you have any
suggestions for improvements.
```

# Prerequisites

* Java 8
* Maven 3.x
* Git (http://git-scm.com)
* NodeJS (http://nodejs.org/) 0.10+
* Node Package Manager (http://npmjs.org)
* Grunt (http://gruntjs.org)
* Bower (http://bower.io)

Grunt and Bower can be installed with Node Package Manager:

```shell
sudo npm install -g grunt-cli bower
```

If Bower fails to download zipped dependencies, make sure it uses decompress-zip >= 0.0.4.
(e.g. check the file /usr/lib/node_modules/bower/node_modules/decompress-zip/package.json,
and reinstall Bower if decompress-zip is too old).

## Windows specific preparation instructions

You can automate the installation of NodeJS and NPM using [Chocolatey package manager]
(https://chocolatey.org).

The package for Node.js can be installed using (this will also install NPM):

    C:\> choco install nodejs.install

__Note:__ Due to a [bug] (http://jira.codehaus.org/browse/MEXEC-137) in the exec-maven-plugin the Maven build of the
API module will fail as the exec-maven-plugin is unable to find the __grunt__ and __bower__ commands. To fix this, do
the following:

* open folder `C:\Users\USER\AppData\Roaming\npm`
* copy __grunt.cmd__ to __grunt.bat__
* copy __bower.cmd__ to __bower.bat__

More information can be found [here] (http://stackoverflow.com/questions/22393785/exec-maven-plugin-says-cannot-run-specified-program-even-though-it-is-on-the-pa/22395703#22395703).

Installation of Git is also possible using Chocolatey:

    C:\> choco install git.install


# Getting Started

## SVN checkout

To get started with the Hippo Essentials, checkout the code. You have two options to check out
the project. The example commands below use the potentially unstable trunk snapshot. Consider
using a tag instead.

### Read-only
```shell
svn co http://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk essentials
```

### Read-write (you'll need Hippo SVN account for this)
```shell
svn co https://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk essentials
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
svn co http://svn.onehippo.org/repos/hippo/hippo-cms7/archetype/trunk/ archetype
cd archetype
mvn clean install
```

### Generate a new Hippo project from the archetype (use appropriate archetype version):
```shell
mvn archetype:generate -D "archetypeGroupId=org.onehippo.cms7" -D "archetypeArtifactId=hippo-project-archetype" -D "archetypeVersion=[archetype version]"
```

##Running locally


This project uses the Maven Cargo plugin to run the CMS, Website and Essentials dashboard locally in Tomcat.
From the project root folder, execute:

```shell
mvn clean verify
mvn -P cargo.run -Drepo.path=storage
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

Essentials depends on the automatic export feature being enabled, which is the archetype-generated Hippo
project's default setting. You can change the setting temporarily in the upper right corner in the CMS,
or permanently in your project's file
`./bootstrap/configuration/src/main/resources/configuration/modules/autoexport-module.xml`

##Copyright and license

Copyright 2013-2015 Hippo B.V.
Distributed under the [Apache 2.0 license](http://svn.onehippo.org/repos/hippo/hippo-cms7/essentials/trunk/LICENSE).

