@ECHO OFF
REM This script assumes that it's located in and executed from the
REM hippo-ecm\tools\eclipse directory.

cd ..\..\

cd package\war\src\main\webapp
junction skin ..\..\..\..\skin\src\main\webapp
junction layout ..\..\..\..\layout\src\main\webapp
junction xinha ..\..\..\..\..\addon\xinha\src\main\webapp

cd ..\..\..\..\..\quickstart\war\src\main\webapp
junction skin ..\..\..\..\..\package\skin\src\main\webapp
junction layout ..\..\..\..\..\package\layout\src\main\webapp
junction xinha ..\..\..\..\..\addon\xinha\src\main\webapp

cd ..\..\..\..\..\addon\xinha\src\main\webapp\xinha\plugins
junction AutoSave ..\..\..\..\..\webResources\xinha\plugins\AutoSave
junction AutoResize ..\..\..\..\..\webResources\xinha\plugins\AutoResize
junction CreateLink ..\..\..\..\..\webResources\xinha\plugins\CreateLink
junction CreateExternalLink ..\..\..\..\..\webResources\xinha\plugins\CreateExternalLink
junction InsertImage ..\..\..\..\..\webResources\xinha\plugins\InsertImage
junction FullscreenCompatible ..\..\..\..\..\webResources\xinha\plugins\FullscreenCompatible

cd ..\skins
junction hippo-lite ..\..\..\..\..\webResources\xinha\skins\hippo-lite
junction hippo-gray ..\..\..\..\..\webResources\xinha\skins\hippo-gray

cd ..\iconsets 
junction Hippo ..\..\..\..\..\webResources\xinha\iconsets\Hippo