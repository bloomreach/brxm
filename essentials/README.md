# Hippo Essentials
Hippo Essentials is for developers who want to setup a new [Hippo CMS](http://www.onehippo.org) project and it enables
them to kickstart their project in a matter of minutes, to benefit from our best practices and to easily add Enterprise
or community plugins from the Hippo Marketplace.

```
Hippo Essentials is in BETA stage and is still undergoing testing and changes before its official release.
Please use the Hippo Essentials feedback form to inform us if you encounter any bugs/glitches or if you have any
suggestions for improvements.
```

# Getting Started

## Git checkout

To get started with the Hippo Essentials checkout the current master branch. You have two options to check out
the project.


### Clone project *

One option is to use HTTPS* (see NOTES below).
The second option is to use a SSH clone. By using the SSH clone, you don't have to provide your username
and password. You just have to create a SSH key and configure the key in your GIT account. There are
[instructions on how to create your SSH key](https://help.github.com/articles/generating-ssh-keys) on Git
Hub. In order to make use of your SSH key, you have to make sure you use the following clone command:

```
$ git clone git@github.com:onehippo/essentials.git
```
### Build the essentials components:
```
$ cd essentials
$ mvn clean install
```

### Validate license headers:
```
$ mvn clean && mvn validate -Ppedantic
```

### Create and install archetype locally :
```
$ cd essentials/archetype
$ mvn clean install
```

### run archetype

```shell
mvn archetype:generate -D "archetypeGroupId=org.onehippo.cms7" -D "archetypeArtifactId=essentials-archetype-website" -D "archetypeVersion=1.01.05-SNAPSHOT"
```

## Working with Git

Create local working branch to work on:
```
$ git checkout -b YOUR_LOCAL_BRANCH_NAME
```

Now work on files, use git status to & git add and git commit files.
NOTE: This can be done within Intellij as well, just commit files

Push your changes to remote repository, -u option is to create remote tracking branch:

```
$ git push -u origin YOUR_LOCAL_BRANCH_NAME
```
This will make a remote branch, visible to other team members.

Once you are make changes,  use git push to push your changes to above mentioned remote branch.
Once you are ready to integrate your work into master, request a pull request through GITHUB website.

NOTE: If you wanna merge your changes yourself do following:


```
$ git checkout master
$ git pull
$ git pull origin YOUR_LOCAL_BRANCH_NAME
$ git push
```

To delete remote branch:
```
$ git push origin --delete  YOUR_LOCAL_BRANCH_NAME
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
 * Essentials dashboard at http://localhost:8080/essentials

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

Copyright 2013-2014 Hippo B.V
Distributed under the [Apache 2.0 license](https://github.com/onehippo/essentials/blob/master/LICENSE).



## NOTES:

### Cloning project using HTTPS clone

The first option is to use a HTTPS clone. You have to provide your GIT username and password to be able
to perform GIT operations. The clone can be created by the following command:

```
$ git clone https://github.com/onehippo/essentials.git
```
