This artifact contains the CKEditor sources, located in 'src/main/resources/ckeditor',
and the optimized version of these sources, located under 'src/main/resources/ckeditor/optimized'.

The non-optimized sources are only used when Wicket runs in development mode.

Both sources are maintained at Github at https://github.com/onehippo/ckeditor.
Do not modify the sources in this artifact.

Version
-------
The version number of the included CKEditor sources can be found in the optimizer ckeditor.js file:

  $ grep -Po 'version:"[0-9.]+"' src/main/resources/ckeditor/optimized/ckeditor.js
  version:"4.3.0.1"

Updating CKEditor sources
-------------------------

When a new tag is available of CKEditor for Hippo CMS, it can replace the sources in this Maven artifact.
The following recipe assumes you start in the directory 'cms/richtext/ckeditor/resources'.

1. Get the tag to copy

   $ cd /tmp
   $ git clone https://github.com/onehippo/ckeditor.git
   $ cd ckeditor
   $ git checkout hippo/4.3.0.1
     (or another tag)
   $ cd -

2. Remove the existing sources

   $ rm -rf src/main/resources/ckeditor

3. Copy the sources in the tag

   $ cp -r /tmp/ckeditor/* src/main/resources/ckeditor

4. Build the tag

   $ /tmp/ckeditor/dev/builder/build.sh

5. Copy the generated optimized sources too

  $ cp -r /tmp/ckeditor/dev/builder/release/ckeditor/* src/main/resources/ckeditor/optimized

6. Commit the updated sources

Creating table with CKEditor widget information
-----------------------------------------------

The script 'create-widget-table.js' can be used to create an HTML table with information about
all CKEditor widgets (buttons, comboboxes etc.) that are available in a CKEditor distribution.
This table can then be included in end-user documentation.
