#!/bin/sh
# This script assumes that it's located in the hippo-ecm/tools/eclipse directory. 
 
cd $(dirname $0) 
cd ../..

cd package/war/src/main/webapp
ln -s -v ../../../../skin/src/main/webapp skin
ln -s -v ../../../../layout/src/main/webapp layout
ln -s -v ../../../../../addon/xinha/src/main/webapp xinha

cd ../../../../../quickstart/war/src/main/webapp
ln -s -v ../../../../../package/skin/src/main/webapp skin
ln -s -v ../../../../../package/layout/src/main/webapp layout
ln -s -v ../../../../../addon/xinha/src/main/webapp xinha

cd ../../../../../addon/xinha/src/main/webapp/xinha/plugins
ln -s -v ../../../../../webResources/xinha/plugins/AutoSave AutoSave
ln -s -v ../../../../../webResources/xinha/plugins/AutoResize AutoResize
ln -s -v ../../../../../webResources/xinha/plugins/CreateLink CreateLink
ln -s -v ../../../../../webResources/xinha/plugins/CreateExternalLink CreateExternalLink
ln -s -v ../../../../../webResources/xinha/plugins/InsertImage InsertImage
ln -s -v ../../../../../webResources/xinha/plugins/FullscreenCompatible FullscreenCompatible

cd ../skins
ln -s -v ../../../../../webResources/xinha/skins/hippo-gray hippo-gray
