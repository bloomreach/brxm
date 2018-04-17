#!/bin/bash

#  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Usage Instructions:
#
# Unzip the translations to ./import/*.xlsx
# Update release and locales vars as needed
# Run ./import.sh
# Review log output
# If no errors, git commit and push the 2 translation modules

locales="nl de fr zh es"
release="12.2.0"

mkdir target

git clone git@code.onehippo.org:cms-community-dev/hippo-cms-translations.git target/hippo-cms-translations
git clone git@code.onehippo.org:cms-enterprise/hippo-cms-enterprise-translations.git target/hippo-cms-enterprise-translations

modules="hippo-cms-translations hippo-cms-enterprise-translations"

for module in $modules; do
    for locale in $locales; do
	    filename=../import/${module}_${release}_${locale}.xlsx
        mvn -N hippo-cms-l10n:import -Dlocale=$locale -Dfile=$filename -f target/$module/pom.xml
    done
done

