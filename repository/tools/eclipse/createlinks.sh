#!/bin/sh
# This script assumes that it's located in the hippo-ecm/tools/eclipse directory. 
 
cd $(dirname $0) 
cd ../..

ln -s package/skin/src/main/webapp  package/war/src/main/webapp/skin
ln -s addon/xinha/src/main/webapp   package/war/src/main/webapp/xinha

ln -s package/skin/src/main/webapp  quickstart/war/src/main/webapp/skin
ln -s addon/xinha/src/main/webapp   quickstart/war/src/main/webapp/xinha

ln -s addon/xinha/webResources/xinha/plugins/AutoSave     addon/xinha/src/main/webapp/xinha/plugins/AutoSave
ln -s addon/xinha/webResources/xinha/plugins/AutoResize   addon/xinha/src/main/webapp/xinha/plugins/AutoResize
ln -s addon/xinha/webResources/xinha/plugins/CustomLinker addon/xinha/src/main/webapp/xinha/plugins/CustomLinker
ln -s addon/xinha/webResources/xinha/plugins/ImagePicker  addon/xinha/src/main/webapp/xinha/plugins/ImagePicker
ln -s addon/xinha/webResources/xinha/skins/hippo-gray     addon/xinha/src/main/webapp/xinha/skins/hippo-gray
