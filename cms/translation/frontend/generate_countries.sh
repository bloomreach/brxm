#!/bin/sh
#
# Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

# use script to generate countries.css

echo ".x-tree-node-expanded img {"
echo "  background-image: url(icons/folder_open/folder-open-16.png);"
echo "}"
echo ".x-tree-node-collapsed img {"
echo "  background-image: url(icons/folder_closed/folder-closed-16.png);"
echo "}"

for file in `ls icons/flags | cut -d_ -f2 | cut -d. -f1`
do
  echo ".x-tree-node-expanded img.hippo-translation-country-$file {"
  echo "  background-image: url(icons/folder_open/folder-open-16_$file.png);"
  echo "}"

  echo ".x-tree-node-collapsed img.hippo-translation-country-$file {"
  echo "  background-image: url(icons/folder_closed/folder-closed-16_$file.png);"
  echo "}"
done
