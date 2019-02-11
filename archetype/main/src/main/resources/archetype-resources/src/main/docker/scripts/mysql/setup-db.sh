#!/usr/bin/env bash

set -e

sed --in-place=.backup 's/@mysql.host@/'"$MYSQL_DB_HOST"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@mysql.port@/'"$MYSQL_DB_PORT"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@mysql.username@/'"$MYSQL_DB_USER"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@mysql.password@/'"$MYSQL_DB_PASSWORD"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@mysql.repo.db@/'"$MYSQL_DB_NAME"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@mysql.driver@/'"$MYSQL_DB_DRIVER"'/' /usr/local/tomcat/conf/context-$profile.xml

sed --in-place=.backup 's/@mysql.repo.db@/'"$MYSQL_DB_NAME"'/' /usr/local/tomcat/conf/repository-$profile.xml


#copy db driver to /usr/local/tomcat/common/lib
cp -R /brxm/db-drivers/$profile/. /usr/local/tomcat/common/lib/