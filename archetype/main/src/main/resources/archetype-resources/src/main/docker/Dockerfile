FROM ${base}

LABEL PROJECT=${project.artifactId}

RUN mkdir -p /brxm/bin

# In maven/ the files as specified in the <assembly> section is stored and need to be added manually
COPY maven/common /usr/local/tomcat/common/
COPY maven/shared /usr/local/tomcat/shared/
COPY maven/webapps /usr/local/tomcat/webapps/
COPY maven/conf /usr/local/tomcat/conf/
COPY maven/scripts /brxm/bin
COPY maven/db-drivers /brxm/db-drivers

RUN mkdir -p /usr/local/tomcat/common/classes
RUN mkdir -p /usr/local/tomcat/shared/classes

# Delete default & unused war files
RUN rm -rf /usr/local/tomcat/webapps/examples
RUN rm -rf /usr/local/tomcat/webapps/docs

# Define a non-root user with limited permissions
RUN adduser -S -u ${container.user.groupId} ${container.user}
RUN chown -R ${container.user} /usr/local/tomcat
RUN chown -R ${container.user} /brxm
USER ${container.user}

RUN ["chmod", "+x", "/brxm/bin/docker-entrypoint.sh"]

ENTRYPOINT ["/brxm/bin/docker-entrypoint.sh"]