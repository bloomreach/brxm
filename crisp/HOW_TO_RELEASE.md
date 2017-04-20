How to Release
==============

Release using gitflow.

References:
- http://danielkummer.github.io/git-flow-cheatsheet/
- http://nvie.com/posts/a-successful-git-branching-model/

## Init gitflow

For the first time, you need to run ```git flow init``` and accept the default settings.

## Start a new release

        git flow release start RELEASE [BASE]

  For example,

        git flow release start 1.2.3 develop

  You're now moved to a new branch, ```release/1.2.3``` automatically.

# Set release versions

  Again, you're already in a new branch, ```release/1.2.3``` automatically.

  Now set the release version. For example,

        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="1.2.3"

  Also, set the release version in the demo folder:

        cd demo
        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="1.2.3"
        cd ..

  **NOTE**: ```demo``` is a child folder managed in the same git repository, but not a maven subproject.
            That's why you set the maven versions separately.

Commit the changes. For example,

        git commit -a -m "Setting release version to 1.2.3."

## Publish the release branch

        git flow release publish RELEASE

  For example,

        git flow release publish 1.2.3

## Finish the release

        git flow release finish RELEASE

  For example,

        git flow release finish 1.2.3

  This finishing probably removed the temporary ```release/1.2.3``` local branch.

## Push ```master``` branch and bump up version in ```develop``` branch and push it.

  For example,

        git checkout master
        git push origin master

        git checkout develop
        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="1.2.4-SNAPSHOT"

        cd demo
        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="1.2.4-SNAPSHOT"

        cd ..
        git commit -a -m "bump up version for next dev cycle."

        git push origin develop
        git branch --set-upstream-to=origin/develop develop

  **NOTE**: Change the default branch to ```develop```.
            If you open your project in gitlab web UI, then go to the gear icon on the right, then "Edit Project".
            You can set the default branch to the ```develop``` for the project.

# Push the newly generated tag.

  For example,

        git tag -l
        git push origin 1.2.3

  You may also remove the remote (temporary) release branch as well (because there's a new tag pushed):

        git push origin :release/1.2.3


## Deploy the release tag to Maven Repository

  If you need to deploy a tag not existing in your local git repo, please make sure the tag exists beforehand:

        git fetch --all --tags --prune

  List the tags:

        git tag -l

  You may check out a specific tag to a local branch temporarily. For example,

        git checkout tags/1.2.3 -b tag-1.2.3

  Now, deploy it to Maven repository from the specific tag branch:

        mvn deploy

  You may remove the local tag branch.

        git checkout develop
        git branch -D tag-1.2.3
