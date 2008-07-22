@ECHO OFF
REM This script assumes that it's located in and executed from the
REM hippo-ecm/tools/eclipse directory.

cd ..\..\
cd package\war\src\main\webapp
junction skin ..\..\..\..\skin\src\main\webapp
junction xinha ..\..\..\..\..\addon\xinha\src\main\webapp
cd ..\..\..\..\..
cd addon\xinha\src\main\webapp\xinha\plugins
junction AutoSave ..\..\..\..\..\webResources\xinha\plugins\AutoSave
junction AutoResize ..\..\..\..\..\webResources\xinha\plugins\AutoResize
junction CustomLinker ..\..\..\..\..\webResources\xinha\plugins\CustomLinker
junction ImagePicker ..\..\..\..\..\webResources\xinha\plugins\ImagePicker
cd ..\skins
junction hippo-gray ..\..\..\..\..\webResources\xinha\skins\hippo-gray
