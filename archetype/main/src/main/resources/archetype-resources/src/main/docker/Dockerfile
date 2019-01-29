FROM ${base}

LABEL PROJECT=${project.artifactId}

# Prepare dirs
# Delete default & unused war files
# Define a non-root user with limited permissions
# Non-root user should own tomcat & /brxm dirs
RUN mkdir -p \
        /brxm/bin \
        /usr/local/tomcat/common/classes \
        /usr/local/tomcat/shared/classes \
    && rm -rf \
        /usr/local/tomcat/webapps/docs \
        /usr/local/tomcat/webapps/examples \
        /usr/local/tomcat/webapps/host-manager \
        /usr/local/tomcat/webapps/manager \
        /usr/local/tomcat/webapps/ROOT \
    && addgroup -g ${container.user.groupId} -S ${container.user} \
    && adduser -G ${container.user} -u ${container.user.groupId} -S ${container.user} \
    && chown -R ${container.user} /usr/local/tomcat /brxm

USER ${container.user}

# In maven/ the files as specified in the <assembly> section are stored and need to be added manually
# COPY in reverse order of expected change frequency, for optimal docker build caching
COPY --chown=${container.user.groupId}:${container.user.groupId} maven/common /usr/local/tomcat/common/
COPY --chown=${container.user.groupId}:${container.user.groupId} maven/db-drivers /brxm/db-drivers
COPY --chown=${container.user.groupId}:${container.user.groupId} maven/scripts /brxm/bin

# Entrypoint script applies env-vars to config, then runs tomcat
ENTRYPOINT ["/brxm/bin/docker-entrypoint.sh"]

COPY --chown=${container.user.groupId}:${container.user.groupId} maven/conf /usr/local/tomcat/conf/
COPY --chown=${container.user.groupId}:${container.user.groupId} maven/shared /usr/local/tomcat/shared/
COPY --chown=${container.user.groupId}:${container.user.groupId} maven/webapps /usr/local/tomcat/webapps/