#!/bin/sh
#
# Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#  http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# This script assumes that 
# - it's located in the cms/tools/eclipse directory. 
# - the quickstart project is under bundle/quickstart

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
create_link $BASEDIR/skin/src/main/webapp   $BASEDIR/../bundle/package/war/src/main/webapp/skin
create_link $BASEDIR/layout/src/main/webapp $BASEDIR/../bundle/package/war/src/main/webapp/layout
echo

echo "Creating links in quickstart war project:"
create_link $BASEDIR/skin/src/main/webapp   $BASEDIR/../bundle/quickstart/war/src/main/webapp/skin
create_link $BASEDIR/layout/src/main/webapp $BASEDIR/../bundle/quickstart/war/src/main/webapp/layout
echo

exit 0;


