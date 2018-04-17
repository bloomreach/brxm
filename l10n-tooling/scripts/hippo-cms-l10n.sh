#!/bin/bash

#  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

locales="nl de fr zh es"
release="12.3.0"
workingfolder_base="$HOME/tmp"
branch="master"

show_help() {
cat << EOF
Usage: ${0##*/} command [-l LOCALES] [-r RELEASE] [-w WORKINGFOLDER] [-b BRANCH]
Script to automate common translation tasks.
Requires the environment variable \$HIPPO_CODE_HOME to be set.

    command can be "export", "import", "stats" or "help"

    export: exports pending and full translations from community and enterprise
            registry into single zip file per locale in working folder
    import: imports translations into registry, reads from working folder
    stats:  displays statistics on pending translations
    help:   displays this help message

    -l LOCALES       list of locales to process, or "$locales" if unspecified
    -r RELEASE       release, or "$release" if unspecified
    -w WORKINGFOLDER working folder, or "$workingfolder_base" if unspecified
    -b BRANCH        branch of the translation registries, or "$branch" if unspecified
EOF
}

while getopts "l:i:s:b:" opt; do
    case "$opt" in
    l)  locales=$OPTARG
        ;;
    r)  release=$OPTARG
        ;;
    w)  workingfolder_base=$OPTARG
        ;;
    b)  branch=$OPTARG
        ;;
    esac
done

export_single_file() {
    locale=$1
    group_name=$2
    full=$3

    folder=$workingfolder/$locale
    mkdir -p $folder
    if [ $? -ne 0 ]; then
        echo "Error creating export folder $folder, aborting"
        exit 1
    fi

    full_string=""
    if [ "$full" == "true" ]; then
        full_string="-full"
    fi

    filename=$folder/hippo-cms-$release-$group_name-$locale$full_string.xlsx
    echo "Exporting $filename"

    mvn hippo-cms-l10n:export -N -Dformat=excel -Dlocale=$locale -Dfull=$full > /dev/null
    if [ $? -ne 0 ]; then
        echo "Error creating export for locale $locale for $group_name, aborting"
        exit 1
    fi

    cp -n export_$locale.xlsx $filename
    if [ $? -ne 0 ]; then
        echo "Error copying file for locale $locale for $group_name, aborting"
        exit 1
    fi
}

export_all_files_for_group() {
    group_folder=$1
    group_name=$2

    cd $group_folder

    for locale in $locales; do
        export_single_file $locale $group_name "false"
        export_single_file $locale $group_name "true"
    done
}

export_translations() {
    if [ -d $workingfolder ]; then
        echo "$workingfolder already exists, aborting"
        exit
    fi

    mkdir -p $workingfolder

    export_all_files_for_group $community "community"
    export_all_files_for_group $enterprise "enterprise"

    cd $workingfolder

    for locale in $locales; do
        zip -r --quiet $locale.zip $locale
        if [ $? -ne 0 ]; then
            echo "Error copying file for locale $locale in $folder, aborting"
        fi
    done
}

import_single_file() {
    locale=$1
    group_name=$2

    folder=$workingfolder/$locale
    if [ ! -d $folder ]; then
        echo "Cannot find '$folder', aborting"
        exit 1
    fi

    filename=$folder/hippo-cms-$release-$group_name-$locale.xlsx
    echo "Importing $filename"

    mvn hippo-cms-l10n:import -N -Dformat=excel -Dlocale=$locale -Dfile=$filename
    if [ $? -ne 0 ]; then
        echo "Error importing '$filename' for locale $locale for $group_name, aborting"
        exit 1
    fi
}

import_all_files_for_group() {
    group_folder=$1
    group_name=$2

    cd $group_folder

    for locale in $locales; do
        import_single_file $locale $group_name
    done
}

import_translations() {
    if [ ! -d $workingfolder ]; then
        echo "folder '$workingfolder' does not exist, aborting"
        exit
    fi

    import_all_files_for_group $community "community"
    import_all_files_for_group $enterprise "enterprise"
}

translation_stats() {
    for group in $community $enterprise; do
        short_name="${group/$HIPPO_CODE_HOME\//}"
        echo Translation statistics for $short_name \(locale, lines, words, characters\):
        cd $group
        for locale in $locales; do
            echo -n "$locale"
            mvn -N hippo-cms-l10n:export -Dformat=csv -Dlocale=$locale > /dev/null
            cat export_$locale.csv | tail -n +2 | sed -e 's/^[^,]*,"\(.*\)",.*/\1/' -e t -e 's/^[^,]*,\(.*\),.*/\1/' | wc
        done
    done
}

if [ -z $HIPPO_CODE_HOME ]; then
    echo "Environment variable \$HIPPO_CODE_HOME is not set"
    exit 1
fi

if [ ! -d $HIPPO_CODE_HOME ]; then
    echo "Environment variable \$HIPPO_CODE_HOME does not point to a directory"
    exit 1
fi

community=$HIPPO_CODE_HOME/cms-community/hippo-cms-translations/$branch
enterprise=$HIPPO_CODE_HOME/cms-enterprise/hippo-cms-enterprise-translations/$branch

if [ ! -d $community ]; then
    echo "folder '$community' does not exist, aborting"
    exit
fi
if [ ! -d $enterprise ]; then
    echo "folder '$enterprise' does not exist, aborting"
    exit
fi

# import/export_translations will check state of workingfolder
workingfolder=$workingfolder_base/$release

case "$1" in
    export) export_translations
            ;;
    import) import_translations
            ;;
    stats)  translation_stats
            ;;
    help)   show_help
            ;;
    "")     show_help
            ;;
esac
