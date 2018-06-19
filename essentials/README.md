# Hippo Essentials
Hippo Essentials is for developers who want to setup a new [Hippo CMS](https://www.onehippo.org) project. It enables
them to kickstart their project in a matter of minutes, to benefit from our best practices and to easily add Enterprise
or community plugins from the Hippo Marketplace.

```
Please use the Hippo Essentials feedback form to inform us if you encounter any bugs/glitches or if you have any
suggestions for improvements.
```

# Getting Started

## Code checkout

To get started with the Hippo Essentials, checkout the code. You have two options to check out
the project. The example commands below use the potentially unstable trunk snapshot. Consider
using a tag instead.

### Read-only
```shell
git clone git@code.onehippo.org:cms-community/hippo-essentials.git
```

### Read-write (you'll need Hippo GIT account for this)
```shell
git clone git@code.onehippo.org:cms-community/hippo-essentials.git
```

### Build the essentials components:
```shell
cd hippo-essentials
mvn clean install
```

### Validate license headers:
```shell
mvn clean && mvn validate -Ppedantic
```

### Create and install archetype locally:
```shell
git clone git@code.onehippo.org:cms-community/hippo-project-archetype.git
cd hippo-project-archetype
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
`./repository-data/config/src/main/resources/configuration/modules/autoexport-module.xml`

##Copyright and license

Copyright 2013-2016 Hippo B.V.
Distributed under the [Apache 2.0 license](https://code.onehippo.org/cms-community/hippo-essentials/blob/master/LICENSE).

