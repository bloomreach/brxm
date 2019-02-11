#!/usr/bin/env bash

set -e

# perform text replacements to apply environment vars properly to jackrabbit database configuration
source /brxm/bin/$profile/setup-db.sh

# use the appropriate database context info for the selected database type
cp /usr/local/tomcat/conf/context-$profile.xml /usr/local/tomcat/conf/context.xml

# run tomcat
exec /usr/local/tomcat/bin/catalina.sh run
