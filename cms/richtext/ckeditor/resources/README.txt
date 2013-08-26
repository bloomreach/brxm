This artifact contains the CKEditor sources, located in 'src/main/resources/ckeditor'.

Version
-------
The version number of the included CKEditor sources can be found in the changelog file:

  src/main/resources/ckeditor/CHANGES.md

The top entry displays the current version.


Updating CKEditor sources
-------------------------

The CKEditor sources can be updated as follows:

1. Go to http://ckeditor.com/builder.
2. Click 'Upload build-config.js' and upload the following file:

     src/main/resources/ckeditor/build-config.js

3. Modify the selected plugins, skins and/or languages.
4. Tick the 'I agree with the Terms' checkbox.
5. Download the *optimized* version of the sources.
6. Extract the downloaded zip file and replace all existing CKEditor sources:

     $ rm -rf src/main/resources/ckeditor
     $ unzip -d src/main/resources ckeditor_XXX.zip

7. Remove the 'samples' directory:

     $ rm -rf src/main/resources/ckeditor/samples

8. Revert the 'icons' directory of the CodeMirror plugin:

     $ svn revert src/main/resources/ckeditor/plugins/codemirror/icons

Step 8 is needed because the current CKEditor builder does not include the 'icons' directory
of the CodeMirror plugin by default.
