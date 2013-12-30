#!/bin/bash

function log() {
  echo "===== $1"
}

function ckeditorVersion() {
  grep -Po 'version:"[0-9.h-]+"' ./src/main/resources/ckeditor/optimized/ckeditor.js
}

if [ $# -ne 1 ]; then
  echo "usage: $0 <tag>"
  exit 1
fi

TAG=$1
TMP_DIR=`mktemp -d`
SCRIPT_DIR="$(pwd)/$(dirname $0)"
GITHUB_REPO=https://github.com/onehippo/ckeditor

# Move to the script directory.
echo $SCRIPT_DIR

log "Checking if tag '$TAG' exists..."
curl --output /dev/null --silent --head --fail $GITHUB_REPO/tree/$TAG
if [ $? -ne 0 ]; then
  echo "Error: Github repository $GITHUB_REPO does not contain a tag '$TAG'"
  exit 2
fi

log "Cloning Hippo CKEditor repository into $TMP_DIR..."
cd $TMP_DIR
git clone https://github.com/onehippo/ckeditor.git
cd ckeditor
git checkout $TAG
cd $SCRIPT_DIR

log "Updating sources..."
rm -rf ./src/main/resources/ckeditor/*
cp -r /tmp/ckeditor/* ./src/main/resources/ckeditor/

log "Building optimized files..."
$TMP_DIR/ckeditor/dev/builder/build.sh

log "Updating optimized files..."
mkdir ./src/main/resources/ckeditor/optimized
cp -r $TMP_DIR/ckeditor/dev/builder/release/ckeditor/* ./src/main/resources/ckeditor/optimized/

log "Removing temporary files..."
rm -rf $TMP_DIR

log "Done, updated CKEditor to $(ckeditorVersion)"
