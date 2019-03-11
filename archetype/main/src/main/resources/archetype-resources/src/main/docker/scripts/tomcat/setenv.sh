#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# ${CATALINA_BASE}/bin/setenv.sh
#
# This script is executed automatically by ${catalina.base}/catalina.sh.
#

# Repository configurations
REP_OPTS="-Drepo.path=${REPO_PATH} -Drepo.bootstrap=${REPO_BOOTSTRAP} -Drepo.config=${REPO_CONFIG} -Drepo.autoexport.allowed=${REPO_AUTOEXPORT_ALLOWED}"

# Logging configurations
L4J_OPTS="-Dlog4j.configurationFile=file://${BRXM_PROJECT_PATH}/conf/log4j2-dev.xml -DLog4jContextSelector=org.apache.logging.log4j.core.selector.BasicContextSelector"

# JVM heap size options
JVM_OPTS="-server -Xms${JAVA_MINHEAP} -Xmx${JAVA_MAXHEAP} -XX:+UseG1GC -Djava.util.Arrays.useLegacyMergeSort=true"

# JVM garbage Collector options
VGC_OPTS="-verbosegc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${CATALINA_HOME}/logs/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=2048k"

# JVM heapdump options
DMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${CATALINA_HOME}/temp"

CATALINA_OPTS="${JVM_OPTS} ${VGC_OPTS} ${REP_OPTS} ${DMP_OPTS} ${L4J_OPTS}"
