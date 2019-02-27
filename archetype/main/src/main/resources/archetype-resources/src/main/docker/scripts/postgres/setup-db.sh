#!/usr/bin/env bash

set -e

sed --in-place=.backup 's/@postgres.host@/'"$POSTGRES_DB_HOST"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@postgres.port@/'"$POSTGRES_DB_PORT"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@postgres.username@/'"$POSTGRES_DB_USER"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@postgres.password@/'"$POSTGRES_DB_PASSWORD"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@postgres.repo.db@/'"$POSTGRES_DB_NAME"'/' /usr/local/tomcat/conf/context-$profile.xml
sed --in-place 's/@postgres.driver@/'"$POSTGRES_DB_DRIVER"'/' /usr/local/tomcat/conf/context-$profile.xml

sed --in-place=.backup 's/@postgres.repo.db@/'"$POSTGRES_DB_NAME"'/' /usr/local/tomcat/conf/repository-$profile.xml
sed --in-place 's/@repo.workspace.bundle.cache@/'"$REPO_WORKSPACE_BUNDLE_CACHE"'/' /usr/local/tomcat/conf/repository-$profile.xml
sed --in-place 's/@repo.versioning.bundle.cache@/'"$REPO_VERSIONING_BUNDLE_CACHE"'/' /usr/local/tomcat/conf/repository-$profile.xml

# Initialize jackrabbit cluster node id by setting with an external value or with hostname.
repo_cluster_id=$REPO_CLUSTER_NODE_ID
if [ -z "$repo_cluster_id" ]; then
    repo_cluster_id="$(hostname -f)"
fi
sed --in-place 's/@cluster.node.id@/'"$repo_cluster_id"'/' /usr/local/tomcat/conf/repository-$profile.xml

#copy db driver to /usr/local/tomcat/common/lib
cp -R /brxm/db-drivers/$profile/. /usr/local/tomcat/common/lib/