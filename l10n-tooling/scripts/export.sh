#!/bin/bash

git clone git@code.onehippo.org:cms-community/hippo-cms-translations.git
git clone git@code.onehippo.org:cms-enterprise/hippo-cms-enterprise-translations.git
git clone git@code.onehippo.org:cms-community/hippo-cms-l10n-tooling.git

version="12.2.0"
locales="nl de fr zh es"
releases="hippo-cms-translations hippo-cms-enterprise-translations"

mkdir target

for release in $releases; do
    for locale in $locales; do
	    filename=target/${release}_${version}_${locale}.xlsx
	    echo xx $filename xx
        mvn -N hippo-cms-l10n:export -Dlocale=$locale -f $release/pom.xml
		
		mv $release/export_$locale.xlsx $filename
        #mvn -N hippo-cms-l10n:export -Dlocale=$locale -f $enterprise translations/pom.xml
    done
done

zip -o -j translation_keys_bloomreach_experience_${version}.zip target/*
