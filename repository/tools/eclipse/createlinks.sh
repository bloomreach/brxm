#!/bin/sh
# This script assumes that it's located in the hippo-ecm/tools/eclipse directory. 

LINK=/bin/ln
RM=/bin/rm

BASEDIR=`dirname $0`/../..
BASEDIR=`(cd "$BASEDIR"; pwd)`


cd $BASEDIR
echo -n "Basedir: "
pwd

# create new symlink
create_link() {  
  if [ -z "$2" ]; then
    echo "create_link needs two arguments"
    exit -1;
  fi
  # remove old link if it exists
  if [ -L "$2" ]; then
    #echo "removing old link $2"
    $RM -f "$2"
  fi

  echo "Creating: $2"
  $LINK -s "$1" "$2"
}


echo "Creating links in package war project:"
create_link $BASEDIR/package/skin/src/main/webapp   $BASEDIR/package/war/src/main/webapp/skin
create_link $BASEDIR/package/layout/src/main/webapp $BASEDIR/package/war/src/main/webapp/layout
create_link $BASEDIR/addon/xinha/src/main/webapp    $BASEDIR/package/war/src/main/webapp/xinha
echo

echo "Creating links in quickstart war project:"
create_link $BASEDIR/package/skin/src/main/webapp   $BASEDIR/quickstart/war/src/main/webapp/skin
create_link $BASEDIR/package/layout/src/main/webapp $BASEDIR/quickstart/war/src/main/webapp/layout
create_link $BASEDIR/addon/xinha/src/main/webapp    $BASEDIR/quickstart/war/src/main/webapp/xinha
echo

echo "Creating links for skin resources:"
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/AutoSave             $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/AutoSave
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/AutoResize           $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/AutoResize
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/CreateLink           $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/CreateLink
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/CreateExternalLink   $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/CreateExternalLink
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/InsertImage          $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/InsertImage
create_link $BASEDIR/addon/xinha/webResources/xinha/plugins/FullscreenCompatible $BASEDIR/addon/xinha/src/main/webapp/xinha/plugins/FullscreenCompatible
echo

echo "Creating link for xinha skin 'hippo-gray':"
create_link $BASEDIR/addon/xinha/webResources/xinha/skins/hippo-gray  $BASEDIR/addon/xinha/src/main/webapp/xinha/skins/hippo-gray
echo

echo "Creating link for xinha skin 'hippo-lite':"
create_link $BASEDIR/addon/xinha/webResources/xinha/skins/hippo-lite  $BASEDIR/addon/xinha/src/main/webapp/xinha/skins/hippo-lite
echo

echo "Creating link for xinha iconset:"
create_link $BASEDIR/addon/xinha/webResources/xinha/iconsets/Hippo  $BASEDIR/addon/xinha/src/main/webapp/xinha/iconsets/Hippo
echo

exit 0;


