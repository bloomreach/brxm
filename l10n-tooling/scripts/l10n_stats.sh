#!/bin/bash

for locale in nl de fr zh es; do
  echo Translation statistics for $locale \(lines, words, characters\):
  mvn -N hippo-cms-l10n:export -Dformat=csv -Dlocale=$locale > /dev/null
  cat export_$locale.csv | tail -n +2 | sed -e 's/^[^,]*,"\(.*\)",.*/\1/' -e t -e 's/^[^,]*,\(.*\),.*/\1/' | wc
done
