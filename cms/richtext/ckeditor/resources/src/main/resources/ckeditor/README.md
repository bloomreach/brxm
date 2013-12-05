CKEditor 4 for Hippo CMS
========================

## Hippo-specific modifications

This repository contains Hippo-specific modifications of CKEditor 4.
The build includes only the plugins used in Hippo CMS (see dev/builder/build-config.js).

The following external plugins are included:

  - [codemirror](https://github.com/w8tcha/CKEditor-CodeMirror-Plugin)
  - [wordcount](https://github.com/w8tcha/CKEditor-WordCount-Plugin)
  - [youtube](https://github.com/fonini/ckeditor-youtube-plugin)

## Versions

A Hippo-specific CKEditor build adds a 1-based nano version to the CKEditor version it extends, prefix with `-h`.
For example, version `4.3.0-h1` extends CKEditor `4.3.0`.

Each branch `hippo/<version>` contains all commits in the CKEditor branch `release/<version>`
plus all Hippo-specific modifications.

A release is available in a tag are named `hippo/<version>`, e.g. `hippo/4.3.0-h1`.

The version number is included in the generated code. Be sure to bump the `BUILD_VERSION` variable in the script
`/dev/builder/build.sh` after tagging a release.

### Branches for external plugins

The Git repository of each external plugin is available as a remote named after the plugin.
Its master branch is available locally as `<plugin>/master`. For example, the CodeMirror master
branch is available locally as `codemirror/master`. This allows easy Hippo-specific modifications
of the plugin code, if needed.

Only a part of each external plugin's code has to be included in the Hippo CKEditor build,
i.e. the part that should to into the CKEditor subdirectory `plugins/XXX`. The history of that
part is kept in a branch `XXX/plugin` and included as a subtree merge under the directory `plugins/XXX`.

For example, all CodeMirror plugin code is located in the branch `codemirror/master`
under the directory `codemirror`. All commits that affect that subdirectory are kept
in the branch `codemirror/plugin`. The code in the branch `codemirror/plugin` is then
included in a Hippo CKEditor branch under the directory `plugins/codemirror`.

## Adding a new external plugin

The following example adds a fictitious external plugin called 'example' to the Hippo CKEditor 4.3.x build.
Its Git repository contains a subdirectory `code` that should go into the CKEditor directory `plugins/example`.

    > git remote add example <remote url>
    > git fetch example
    > git checkout -b example/master example/master
    > git subtree split --prefix=code/ -b example/plugin
    > git checkout hippo/4.3.x
    > git read-tree --prefix=plugins/example/ -u example/plugin

Add the 'example' plugin to the file `dev/builder/build-config.js` to include it in the Hippo CKEditor build.

## The remainder of this file contains the unmodified CKEditor README

## Development Code

This repository contains the development version of CKEditor.

**Attention:** The code in this repository should be used locally and for
development purposes only. We don't recommend distributing it on remote websites
because the user experience will be very limited. For that purpose, you should
build it (see below) or use an official release instead, available on the
[CKEditor website](http://ckeditor.com).

### Code Installation

There is no special installation procedure to install the development code.
Simply clone it on any local directory and you're set.

### Available Branches

This repository contains the following branches:

  - **master**: development of the upcoming minor release.
  - **major**: development of the upcoming major release.
  - **stable**: latest stable release tag point (non-beta).
  - **latest**: latest release tag point (including betas).
  - **release/A.B.x** (e.g. 4.0.x, 4.1.x): release freeze, tests and tagging.
    Hotfixing.

(*) Note that both **master** and **major** are under heavy development. Their
code didn't pass the release testing phase so it may be unstable.

Additionally, all releases will have their relative tags in this form: 4.0,
4.0.1, etc.

### Samples

The `samples/` folder contains a good set of examples that can be used
to test your installation. It can also be a precious resource for learning
some aspects of the CKEditor JavaScript API and its integration on web pages.

### Code Structure

The development code contains the following main elements:

  - Main coding folders:
    - `core/`: the core API of CKEditor. Alone, it does nothing, but
    it provides the entire JavaScript API that makes the magic happen.
    - `plugins/`: contains most of the plugins maintained by the CKEditor core team.
    - `skin/`: contains the official default skin of CKEditor.
    - `dev/`: contains "developer tools".

### Building a Release

A release optimized version of the development code can be easily created
locally. The `dev/builder/build.sh` script can be used for that purpose:

	> ./dev/builder/build.sh

A "release ready" working copy of your development code will be built in the new
`dev/builder/release/` folder. An internet connection is necessary to run the
builder, for its first time at least.

### License

Licensed under the GPL, LGPL and MPL licenses, at your choice.

For full details about license, please check the LICENSE.md file.
