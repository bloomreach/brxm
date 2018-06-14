/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.environment.EnvironmentSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_MOUNT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_PORTMOUNT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_VIRTUALHOST;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP;
import static org.hippoecm.hst.environment.EnvironmentParameters.HST_HOSTS;
import static org.onehippo.cms7.services.HippoServiceRegistry.getService;

public class HostConfigPublisher {

    private static final Logger log = LoggerFactory.getLogger(HostConfigPublisher.class);

    private final EnvironmentSettings environmentSettings;
    private final ExecutorService executorService;
    // ObjectMapper is thread-safe except for some config setters which we don't use
    private final static ObjectMapper objectMapper = new ObjectMapper();

    private Repository repository;
    private Credentials credentials;

    public HostConfigPublisher() {
        environmentSettings = getService(EnvironmentSettings.class);
        if (environmentSettings == null) {
            executorService = null;
            return;
        }
        executorService = newSingleThreadExecutor();
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void asyncPublishHosts(final String hostGroupPath) {
       if (environmentSettings == null) {
           return;
       }
        executorService.submit(new HostConfigPublisherTask(repository, credentials, hostGroupPath, environmentSettings));
    }

    private static class HostConfigPublisherTask implements Runnable {

        private final Repository repository;
        private final Credentials credentials;
        private final String hostGroupPath;
        private EnvironmentSettings environmentSettings;

        private HostConfigPublisherTask(final Repository repository,
                                        final Credentials credentials,
                                        final String hostGroupPath,
                                        final EnvironmentSettings environmentSettings) {
            this.repository = repository;
            this.credentials = credentials;
            this.hostGroupPath = hostGroupPath;
            this.environmentSettings = environmentSettings;
        }

        @Override
        public void run() {
            Session session = null;
            try {
                session = repository.login(credentials);
                final Node root = session.getNode(hostGroupPath);
                if (!root.isNodeType(NODETYPE_HST_VIRTUALHOSTGROUP)) {
                    throw new IllegalArgumentException(String.format("Invalid root '%s' for publishing hosts info.", hostGroupPath));
                }
                List<String> hostList = buildHostsList(root);
                environmentSettings.put(HST_HOSTS, objectMapper.writeValueAsString(hostList));

            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.error("Exception while trying to publish environment host information for '{}'", hostGroupPath, e);
                } else {
                    log.error("Exception while trying to publish environment host information for '{}' : {}", hostGroupPath, e.toString());
                }
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        private List<String> buildHostsList(final Node root) throws RepositoryException {
            List<String> hosts = new ArrayList<>();
            populateHostsList(root, hosts);
            return hosts;
        }

        private void populateHostsList(final Node current, final List<String> hosts) throws RepositoryException {
            for (Node child : new NodeIterable(current.getNodes())) {
                if (child.isNodeType(NODETYPE_HST_VIRTUALHOST)) {
                    populateHostsList(child, hosts);
                } else if (child.isNodeType(NODETYPE_HST_PORTMOUNT) || child.isNodeType(NODETYPE_HST_MOUNT)) {
                    addHost(child.getParent(), hosts);
                }
            }

        }

        private void addHost(final Node leafHostNode, final List<String> hosts) throws RepositoryException {
            final StringBuilder hostName = new StringBuilder(leafHostNode.getName());
            Node current = leafHostNode;
            while(current.getParent().isNodeType(NODETYPE_HST_VIRTUALHOST)) {
                current = current.getParent();
                hostName.append(".").append(current.getName());
            }
            hosts.add(hostName.toString());
        }
    }
}
