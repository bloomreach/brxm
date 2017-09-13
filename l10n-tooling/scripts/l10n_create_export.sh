#!/bin/bash

locales="nl de fr zh es"
indicator="12.1"
destination_base="$HOME/tmp"

show_help() {
cat << EOF
Usage: ${0##*/} [-l LOCALES] [-i INDICATOR] [-d DESTINATION]
Exports the pending and full set of community and enterprise translations.
Requires the environment variable \$HIPPO_CODE_HOME to be set.

    -h             display this help and exit
    -l LOCALES     list of locales to export, or "$locales" if unspecified
    -i INDICATOR   indicator, or "$indicator" if unspecified
    -d DESTINATION destination folder, or "$destination_base" if unspecified
EOF
}

while getopts "h?l:i:d:" opt; do
    case "$opt" in
    h)
        show_help
        exit 0
        ;;
    l)  locales=$OPTARG
        ;;
    i)  indicator=$OPTARG
        ;;
    d)  destination_base=$OPTARG
        ;;
    esac
done

create_export() {
    locale=$1
    name=$2
    full=$3

    folder=$destination/$locale
    mkdir -p $folder
    if [ $? -ne 0 ]; then
        echo "Error creating export folder $folder, aborting"
        exit 1
    fi

    full_string=""
    if [ "$full" == "true" ]; then
        full_string="-full"
    fi

    filename=$folder/hippo-cms-$indicator-$name-$locale$full_string.xlsx
    echo "Exporting $filename"

    mvn hippo-cms-l10n:export -N -Dformat=excel -Dlocale=$locale -Dfull=$full > /dev/null
    if [ $? -ne 0 ]; then
        echo "Error creating export for locale $locale for $name, aborting"
        exit 1
    fi

    cp -n export_$locale.xlsx $filename
    if [ $? -ne 0 ]; then
        echo "Error copying file for locale $locale for $name, aborting"
        exit 1
    fi
}

gather_all_exports() {
    folder=$1
    name=$2

    cd $folder

    for locale in $locales; do
        create_export $locale $name "false"
        create_export $locale $name "true"
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

destination=$destination_base/$indicator
if [ -d $destination ]; then
    echo "$destination already exists, aborting"
    exit
fi

mkdir -p $destination

community=$HIPPO_CODE_HOME/cms-community/hippo-cms-translations/master
enterprise=$HIPPO_CODE_HOME/cms-enterprise/hippo-cms-enterprise-translations/master

gather_all_exports $community "community"
gather_all_exports $enterprise "enterprise"

cd $destination

for locale in $locales; do
    zip -r --quiet $locale.zip $locale
    if [ $? -ne 0 ]; then
        echo "Error copying file for locale $locale in $folder, aborting"
    fi
done
