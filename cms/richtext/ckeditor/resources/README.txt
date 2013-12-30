This artifact contains the CKEditor sources, located in 'src/main/resources/ckeditor',
and the optimized version of these sources, located under 'src/main/resources/ckeditor/optimized'.

The non-optimized sources are only used when Wicket runs in development mode.

Both sources are maintained at Github at https://github.com/onehippo/ckeditor.
Do not modify the sources in this artifact.

Version
-------
The version number of the included CKEditor sources can be found in the optimizer ckeditor.js file:

  $ grep -Po 'version:"[0-9.h-]+"' src/main/resources/ckeditor/optimized/ckeditor.js
  version:"4.3.0-h1"

Updating CKEditor sources
-------------------------

When a new tag of CKEditor for Hippo CMS is available, the sources in this Maven artifact can be replaced
by running:

  $ ./update-ckeditor.sh <tag>

Replace <tag> with the tag that should be used. Commit the updated sources when done.

Do not forget to update the CKEditor documentation on Hippo Campus. If plugins have been added and/or removed,
an updated table of toolbar items provided by all plugins can be generated using a script (see below).

Creating table with CKEditor widget information
-----------------------------------------------

The script 'create-toolbar-items-table.js' can be used to create an HTML table with information about
all CKEditor toolbar items (buttons, comboboxes etc.) that are available in a CKEditor distribution.
This table can then be included in end-user documentation.
