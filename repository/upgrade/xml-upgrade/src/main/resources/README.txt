XML Bootstrap Upgrade
=====================
Upgrades bootstrap content from the old handle model to the new one.
The replacements done are:

  Folder:
    hippo:harddocument -> mix:referenceable

  Handle:
    hippo:hardhandle   -> mix:referenceable

  Document Variant:
    hippo:harddocument unpublished : mix:versionable
    hippo:harddocument published : mix:referenceable

  Hippo Translation Node (mixin kept):
      hippotranslation:translation : removed

Usage
=====
The program can be run by starting the bin/xml-upgrade script (on Unix).
(or the xml-upgrade.bat script on Windows)

   bin/xml-upgrade [<folder>]

ps make xml-upgrade executable through chmod a+x

By default, i.e. when no folder is specified, it will traverse all child
folders of the current working directory.  If a (relative or absolute) path to
a folder is provided, it will be traversed.
