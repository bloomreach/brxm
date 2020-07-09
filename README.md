# This is not the branch you're looking for...

Bloomreach only provides the git trees for the release tags of Bloomreach Experience CMS, as explained on https://documentation.bloomreach.com/about/open-source-release-policy.html

To checkout the code for a specific release tag, after cloning this repository, use the following:

## to show the available tags

    git tag

## to checkout a specific tag

    git checkout <tag name>

## to modify a project
If you want to make modifications to a project, for example to create a patch, create a new fork branch from the specific tag like this:

    git checkout -b forked-<tag name> <tag name>

For the latter, also see the **Build from Source** documentation at https://documentation.bloomreach.com/library/development/build-hippo-cms-from-scratch.html
