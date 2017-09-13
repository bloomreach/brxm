#!/bin/bash

locales="nl de fr zh es"
indicator="12.1"
source_base="."
branch="master"

show_help() {
cat << EOF
Usage: ${0##*/} [-l LOCALES] [-i INDICATOR] [-s SOURCE] [-b BRANCH]
Imports the translations into both the enterprise and community translation registry.
Requires the environment variable \$HIPPO_CODE_HOME to be set.

    -h             display this help and exit
    -l LOCALES     list of locales to export, or "$locales" if unspecified
    -i INDICATOR   indicator, or "$indicator" if unspecified
    -s SOURCE      source base folder, or "$source_base" if unspecified
    -b BRANCH      branch where to write the translations to
EOF
}

while getopts "h?l:i:s:b:" opt; do
    case "$opt" in
    h)
        show_help
        exit 0
        ;;
    l)  locales=$OPTARG
        ;;
    i)  indicator=$OPTARG
        ;;
    s)  source_base=$OPTARG
        ;;
    b)  branch=$OPTARG
        ;;
    esac
done

import() {
    locale=$1
    name=$2

    folder=$source/$locale
    if [ ! -d $folder ]; then
        echo "Cannot find '$folder', aborting"
        exit 1
    fi

    filename=$folder/hippo-cms-$indicator-$name-$locale.xlsx
    echo "Importing $filename"

    mvn hippo-cms-l10n:import -N -Dformat=excel -Dlocale=$locale -Dfile=$filename
    if [ $? -ne 0 ]; then
        echo "Error importing '$filename' for locale $locale for $name, aborting"
        exit 1
    fi
}

import_all() {
    folder=$1
    name=$2

    if [ ! -d $folder ]; then
        echo "folder '$folder' does not exist, aborting"
        exit
    fi
    cd $folder

    for locale in $locales; do
        import $locale $name
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

source=$source_base/$indicator
if [ ! -d $source ]; then
    echo "folder '$source' does not exist, aborting"
    exit
fi

community=$HIPPO_CODE_HOME/cms-community/hippo-cms-translations/$branch
enterprise=$HIPPO_CODE_HOME/cms-enterprise/hippo-cms-enterprise-translations/$branch

import_all $community "community"
import_all $enterprise "enterprise"
