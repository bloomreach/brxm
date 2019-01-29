FROM ${docker.base.image}

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
    && addgroup -g ${docker.container.uid} -S ${docker.container.username} \
    && adduser -G ${docker.container.username} -u ${docker.container.uid} -S ${docker.container.username} \
    && chown -R ${docker.container.username} /usr/local/tomcat /brxm

USER ${docker.container.username}

# In maven/ the files as specified in the <assembly> section are stored and need to be added manually
# COPY in reverse order of expected change frequency, for optimal docker build caching
COPY --chown=${docker.container.uid}:${docker.container.uid} maven/common /usr/local/tomcat/common/
COPY --chown=${docker.container.uid}:${docker.container.uid} maven/db-drivers /brxm/db-drivers
COPY --chown=${docker.container.uid}:${docker.container.uid} maven/scripts /brxm/bin
RUN chmod +x /brxm/bin/docker-entrypoint.sh

# Entrypoint script applies env-vars to config, then runs tomcat
ENTRYPOINT ["/brxm/bin/docker-entrypoint.sh"]

COPY --chown=${docker.container.uid}:${docker.container.uid} maven/conf /usr/local/tomcat/conf/
COPY --chown=${docker.container.uid}:${docker.container.uid} maven/shared /usr/local/tomcat/shared/
COPY --chown=${docker.container.uid}:${docker.container.uid} maven/webapps /usr/local/tomcat/webapps/