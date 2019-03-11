#!/usr/bin/env bash

set -e

# perform text replacements to apply environment vars properly to jackrabbit database configuration
source /brxm/bin/$profile/setup-db.sh

if [ "$profile" != "h2" ]
then
    # initialize jackrabbit cluster node id by setting with an external value or with hostname.
    repo_cluster_id=$REPO_CLUSTER_NODE_ID
    if [ -z "$repo_cluster_id" ]
    then
        repo_cluster_id="$(hostname -f)"
    fi
sed --in-place 's/@cluster.node.id@/'"$repo_cluster_id"'/' /usr/local/tomcat/conf/repository-$profile.xml
fi

# update tomcat http max threads variable
sed --in-place=.backup 's/@tomcat.max.threads@/'"$TOMCAT_MAXTHREADS"'/' /usr/local/tomcat/conf/server.xml

# use the appropriate database context info for the selected database type
cp /usr/local/tomcat/conf/context-$profile.xml /usr/local/tomcat/conf/context.xml

# copy setting environment variables script to the related tomcat folder 
cp /brxm/bin/tomcat/setenv.sh /usr/local/tomcat/bin/setenv.sh

# run tomcat
exec /usr/local/tomcat/bin/catalina.sh run
