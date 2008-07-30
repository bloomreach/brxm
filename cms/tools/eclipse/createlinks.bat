@ECHO OFF
REM This script assumes that it's located in and executed from the
REM hippo-ecm\tools\eclipse directory.

cd ..\..\

junction package\war\src\main\webapp\skin package\skin\src\main\webapp  
junction package\war\src\main\webapp\xinha addon\xinha\src\main\webapp   

junction quickstart\war\src\main\webapp\skin package\skin\src\main\webapp  
junction quickstart\war\src\main\webapp\xinha addon\xinha\src\main\webapp   

junction addon\xinha\src\main\webapp\xinha\plugins\AutoSave addon\xinha\webResources\xinha\plugins\AutoSave
junction addon\xinha\src\main\webapp\xinha\plugins\AutoResize addon\xinha\webResources\xinha\plugins\AutoResize
junction addon\xinha\src\main\webapp\xinha\plugins\CustomLinker addon\xinha\webResources\xinha\plugins\CustomLinker
junction addon\xinha\src\main\webapp\xinha\plugins\ImagePicker addon\xinha\webResources\xinha\plugins\ImagePicker
junction addon\xinha\src\main\webapp\xinha\skins\hippo-gray addon\xinha\webResources\xinha\skins\hippo-gray
