#!/usr/bin/env bash

set -e

source /brxm/bin/$profile/setup-db.sh

cp /usr/local/tomcat/conf/context-$profile.xml /usr/local/tomcat/conf/context.xml

exec /usr/local/tomcat/bin/catalina.sh run
