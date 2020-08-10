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

	# set REPO_CONFIG variable if it is not set
    repo_config=$REPO_CONFIG
    if [ -z "$repo_config" ]
    then
        export REPO_CONFIG=file:/usr/local/tomcat/conf/repository-$profile.xml
    fi
fi

# update tomcat http max threads variable
sed --in-place=.backup 's/@tomcat.max.threads@/'"$TOMCAT_MAXTHREADS"'/' /usr/local/tomcat/conf/server.xml

# use the appropriate database context info for the selected database type
cp /usr/local/tomcat/conf/context-$profile.xml /usr/local/tomcat/conf/context.xml

# copy setting environment variables script to the related tomcat folder 
cp /brxm/bin/tomcat/setenv.sh /usr/local/tomcat/bin/setenv.sh

# Unzip the lucene index export if it exists
if [[ -e "${LUCENE_INDEX_FILE_PATH}" ]]; then
  echo "Extracting the lucene index export zip..."
  mkdir -p ${REPO_PATH}/workspaces/default/index/
  unzip ${LUCENE_INDEX_FILE_PATH} -d ${REPO_PATH}/workspaces/default/index/
fi

# run tomcat
exec /usr/local/tomcat/bin/catalina.sh run
