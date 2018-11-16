#!/usr/bin/env bash

# Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

# Releases the UI extension package on NPM central.
#
# Requirements:
# - NPM user account (see https://www.npmjs.com/login)
# - NPM user should be part of the @bloomreach organization (see https://www.npmjs.com/settings/bloomreach/members)
#
# BE CAREFUL:
# - A released package can be un-released with "npm unpublish @bloomreach/ui-extension@<version>"
# - This can only be done within 72 hours after publishing
# - PUBLISHED VERSIONS CAN NEVER BE REUSED AGAIN, so publishing again needs to increase the patch version.

if [ $# -ne 1 ]; then
  echo "Usage: $0 <version>";
  exit 1
fi

version=$1

# Did we run the build?
if [ "`ls -A dist 2>/dev/null | head -1`" = "" ]; then
    echo "Please run the build first"
    exit 1;
fi

# Is NPM installed?
if ! command -v npm >/dev/null 2>&1; then
  echo "Please install NPM"
  exit 1;
fi

# Are we logged in to NPM?
if ! npm whoami >/dev/null 2>&1; then
  echo "Please login to NPM first"
  npm login
fi

# Change version in package.json  file
printf "Setting version: "
if ! npm version "${version}"; then
  echo
  echo "Cannot set version to \"${version}\""
  echo
  exit 1
fi

# Publish package
echo "Publishing package to NPM:"
npm publish --access public

# Revert version bump in package.json
printf "Resetting development version: "
npm version 0.0.0

echo
echo "Published version ${version}, see https://www.npmjs.com/package/@bloomreach/ui-extension?activeTab=versions"
echo
