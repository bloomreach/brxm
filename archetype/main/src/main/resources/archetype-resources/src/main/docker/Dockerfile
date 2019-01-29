FROM ${base}

LABEL PROJECT=${project.artifactId}

# Prepare dirs
# Delete default & unused war files
# Define a non-root user with limited permissions
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
    && adduser -S -u ${container.user.groupId} ${container.user}

# In maven/ the files as specified in the <assembly> section are stored and need to be added manually
# COPY in reverse order of expected change frequency, for optimal docker build caching
COPY maven/common /usr/local/tomcat/common/
COPY maven/db-drivers /brxm/db-drivers
COPY maven/scripts /brxm/bin
COPY maven/conf /usr/local/tomcat/conf/
COPY maven/shared /usr/local/tomcat/shared/
COPY maven/webapps /usr/local/tomcat/webapps/

# Non-root user should own tomcat & /brxm dirs
RUN chown -R ${container.user} /usr/local/tomcat /brxm \
    && chmod +x /brxm/bin/docker-entrypoint.sh
USER ${container.user}

# Apply env vars to config, then run tomcat
ENTRYPOINT ["/brxm/bin/docker-entrypoint.sh"]