#!/bin/sh

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
