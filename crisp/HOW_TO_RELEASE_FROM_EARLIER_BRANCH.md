How to Release From an Earlier Branch
=====================================

Release using git.

## Create a new release branch and bump up versions

        git checkout 0.1.x -b release/0.1.1
        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="0.1.1"
        git commit -a -m "bump up version for release."

## Publish the release branch

        git push -u origin release/0.1.1

## Tag and publish the Tag

        git tag -a 0.1.1 -m "Tagging 0.1.1"
        git tag -l
        git push origin 0.1.1

## Merge the release branch to the base branch

        git checkout 0.1.x
        git merge release/0.1.1

# Bump up the version in the base branch

        git checkout 0.1.x
        mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DgenerateBackupPoms=false -DnewVersion="0.1.2-SNAPSHOT"
        git commit -a -m "bump up version for next dev cycle."
        git push origin 0.1.x

## Remove release branch

        git branch -D release/0.1.1
        git push origin :release/0.1.1

## Deploy the release tag to Maven Repository

  If you need to deploy a tag not existing in your local git repo, please make sure the tag exists beforehand:

        git fetch --all --tags --prune

  List the tags:

        git tag -l

  You may check out a specific tag to a local branch temporarily. For example,

        git checkout tags/0.1.1 -b tag-0.1.1

  Now, deploy it to Maven repository from the specific tag branch:

        mvn deploy

  You may remove the local tag branch.

        git checkout 0.1.x
        git branch -D tag-0.1.1
