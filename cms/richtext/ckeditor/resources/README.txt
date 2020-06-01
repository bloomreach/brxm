This artifact pulls in the Hippo CKEditor sources maintained at https://github.com/onehippo/ckeditor.

All optimized CKEditor resources are located in JAR files under the directory /ckeditor/optimized.

The non-optimized CKEditor resources are located in JAR files under the directory /ckeditor, and
are only used when Wicket runs in development mode.

Creating table with CKEditor widget information
-----------------------------------------------

The script 'create-toolbar-items-table.js' can be used to create an HTML table with information about
all CKEditor toolbar items (buttons, comboboxes etc.) that are available in a CKEditor distribution.
This table can then be included in end-user documentation.
