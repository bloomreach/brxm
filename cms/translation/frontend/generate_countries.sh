#!/bin/sh
#
# Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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

# use script to generate the bottom part of countries.css

for country in `ls ./src/main/java/org/hippoecm/frontend/translation/icons/flags/flag-11x9_*.png | cut -d_ -f2 | cut -d. -f1`
do
  echo ".hippo-translation-country-$country {"
  echo "  background-image: url(icons/flags/flag-11x9_$country.png);"
  echo "}"
done
