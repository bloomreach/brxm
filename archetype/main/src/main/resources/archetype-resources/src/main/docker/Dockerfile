FROM ${docker.brxm.base.image}

LABEL PROJECT=${project.artifactId}

# Default JVM heap size variables
ENV JAVA_MINHEAP "256m"
ENV JAVA_MAXHEAP "512m"

# Default tomcat http max threads variable
ENV TOMCAT_MAXTHREADS "200"

# BrXM project path variable
ENV BRXM_PROJECT_PATH "${docker.brxm.project.path}"

# Default repository settings 
ENV REPO_PATH "${BRXM_PROJECT_PATH}/target/storage"
ENV REPO_CONFIG ""
ENV REPO_BOOTSTRAP "false"
ENV REPO_AUTOEXPORT_ALLOWED "false"

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
    && addgroup --gid ${docker.brxm.container.uid} ${docker.brxm.container.username} \
    && adduser --gid ${docker.brxm.container.uid} --uid ${docker.brxm.container.uid} ${docker.brxm.container.username} \
    && chown -R ${docker.brxm.container.username} /usr/local/tomcat /brxm

USER ${docker.brxm.container.username}

# In maven/ the files as specified in the <assembly> section are stored and need to be added manually
# COPY in reverse order of expected change frequency, for optimal docker build caching
COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/common /usr/local/tomcat/common/
COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/db-drivers /brxm/db-drivers
COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/scripts /brxm/bin
RUN chmod +x /brxm/bin/docker-entrypoint.sh

# Entrypoint script applies env-vars to config, then runs tomcat
ENTRYPOINT ["/brxm/bin/docker-entrypoint.sh"]

COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/conf /usr/local/tomcat/conf/
COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/shared /usr/local/tomcat/shared/
COPY --chown=${docker.brxm.container.uid}:${docker.brxm.container.uid} maven/webapps /usr/local/tomcat/webapps/